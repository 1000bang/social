package com.mysocial.dispatch

import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.config.DispatchProperties
import com.mysocial.instagram.InstagramErrorClassifier
import com.mysocial.instagram.InstagramGraphClient
import com.mysocial.instagram.InstagramMessagingClient
import com.mysocial.settings.AccountSettingsService
import com.mysocial.template.AudienceType
import com.mysocial.template.Template
import com.mysocial.template.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

// 레이트리밋에 근접해 발송을 미룰 때 쓰는 지연 시간. 실패 후 백오프와 달리 재시도 횟수를 소모하지 않는다.
private val RATE_LIMIT_DEFER_DELAY: Duration = Duration.ofMinutes(10)

@Service
class DispatchExecutor(
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val templateRepository: TemplateRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val sendLogRepository: SendLogRepository,
	private val instagramGraphClient: InstagramGraphClient,
	private val instagramMessagingClient: InstagramMessagingClient,
	private val instagramErrorClassifier: InstagramErrorClassifier,
	private val dispatchProperties: DispatchProperties,
	private val accountSettingsService: AccountSettingsService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun replyToNonMatchingComment(templateId: Long, commentId: String, commenterUsername: String?) {
		log.info("비키워드 댓글 답글 진입: templateId={}, commentId={}", templateId, commentId)
		val template = templateRepository.findById(templateId).orElse(null)
		if (template == null) {
			log.warn("비키워드 댓글 답글 처리 불가: template을 찾을 수 없음 templateId={}", templateId)
			return
		}
		val token = latestToken(template.account.id)
		if (token == null) {
			log.warn("비키워드 댓글 답글 처리 불가: 유효한 액세스 토큰 없음 accountId={}", template.account.id)
			return
		}

		runCatching {
			val replyText = MessagePayloadBuilder.applyUsernamePlaceholder(template.resolvedNonKeywordCommentReplyText(), commenterUsername)
			instagramMessagingClient.replyToComment(token, commentId, replyText)
		}.onSuccess {
			log.info("비키워드 댓글 답글 완료: templateId={}, commentId={}", templateId, commentId)
		}.onFailure { ex ->
			log.warn("비키워드 댓글 답글 실패: templateId={}, commentId={}", templateId, commentId, ex)
		}
	}

	@Transactional
	fun sendInitialPrompt(dispatchTargetId: Long) {
		log.info("초기 발송 진입: dispatchTargetId={}", dispatchTargetId)
		val target = dispatchTargetRepository.findById(dispatchTargetId).orElse(null)
		if (target == null) {
			log.warn("초기 발송 처리 불가: dispatchTarget을 찾을 수 없음 dispatchTargetId={}", dispatchTargetId)
			return
		}
		if (target.status != DispatchStatus.PENDING && target.status != DispatchStatus.RETRY_PENDING) {
			log.warn("초기 발송 처리 불가: 대기 상태가 아님 dispatchTargetId={}, status={}", dispatchTargetId, target.status)
			return
		}
		val template = target.template
		val token = latestToken(template.account.id)
		if (token == null) {
			log.warn("초기 발송 처리 불가: 유효한 액세스 토큰 없음 accountId={}", template.account.id)
			return
		}
		if (!hasSendCapacity(template.account.id)) {
			log.info("시간당 발송 한도 근접, 재시도 예약: dispatchTargetId={}", dispatchTargetId)
			target.markRetryPending(DispatchStage.INITIAL_PROMPT, Instant.now().plus(RATE_LIMIT_DEFER_DELAY), countsAsAttempt = false)
			return
		}

		val followPromptText = accountSettingsService.getFollowPromptText(template.account.id)
		val followButtonTitle = accountSettingsService.getFollowButtonTitle(template.account.id)

		runCatching {
			if (target.triggerType == TriggerType.COMMENT) {
				val replyText = MessagePayloadBuilder.applyUsernamePlaceholder(template.resolvedCommentReplyText(), target.resolvedUsername)
				instagramMessagingClient.replyToComment(token, target.platformTriggerId, replyText)
				instagramMessagingClient.sendPrivateReply(
					token,
					target.platformTriggerId,
					MessagePayloadBuilder.promptWithFollowButton(followPromptText, followButtonTitle, target.id, target.resolvedUsername),
				)
			} else {
				instagramMessagingClient.sendDirectMessage(
					token,
					target.recipientPlatformUserId,
					MessagePayloadBuilder.promptWithFollowButton(followPromptText, followButtonTitle, target.id, target.resolvedUsername),
				)
			}
			target.markAwaitingFollowCheck()
		}.onSuccess {
			log.info("초기 발송 완료: dispatchTargetId={}, status={}", dispatchTargetId, target.status)
		}.onFailure { ex ->
			handleSendFailure(target, DispatchStage.INITIAL_PROMPT, null, ex) {
				sendLogRepository.save(
					SendLog(
						template = template,
						audienceType = null,
						recipientPlatformUserId = target.recipientPlatformUserId,
						result = SendResult.FAILED,
						failureReason = ex.message,
					),
				)
			}
		}
	}

	// 재시도 스케줄러가 호출하는 진입점. 실패했던 단계에 맞춰 이어서 처리한다.
	@Transactional
	fun retryDispatch(dispatchTargetId: Long) {
		val target = dispatchTargetRepository.findById(dispatchTargetId).orElse(null)
		if (target == null || target.status != DispatchStatus.RETRY_PENDING) return

		when (target.retryStage) {
			DispatchStage.INITIAL_PROMPT -> sendInitialPrompt(dispatchTargetId)
			DispatchStage.AUDIENCE_MESSAGE -> {
				val audienceType = target.retryAudienceType
				if (audienceType == null) {
					// 팔로우 여부 조회 자체가 실패했던 경우: 처음부터 다시 확인한다.
					handleFollowCheckClick(dispatchTargetId, target.recipientPlatformUserId)
				} else {
					resumeAudienceMessageSend(target, audienceType)
				}
			}
			null -> log.warn("재시도 단계 정보가 없어 처리를 생략합니다: dispatchTargetId={}", dispatchTargetId)
		}
	}

	@Transactional
	fun handleFollowCheckClick(dispatchTargetId: Long, senderPsid: String) {
		log.info("팔로우 확인 버튼 처리 진입: dispatchTargetId={}, senderPsid={}", dispatchTargetId, senderPsid)
		val target = dispatchTargetRepository.findById(dispatchTargetId).orElse(null)
		if (target == null) {
			log.warn("팔로우 확인 버튼 처리 불가: dispatchTarget을 찾을 수 없음 dispatchTargetId={}", dispatchTargetId)
			return
		}
		// 논팔로워로 처리된 뒤에도 버튼을 다시 누르면 팔로우 여부를 재확인해서, 그사이 팔로우했다면 팔로워 메시지를 마저 보낸다.
		if (target.status != DispatchStatus.AWAITING_FOLLOW_CHECK &&
			target.status != DispatchStatus.NON_FOLLOWER_SENT &&
			target.status != DispatchStatus.RETRY_PENDING
		) {
			log.warn("팔로우 확인 버튼 처리 불가: 대기 상태가 아님 dispatchTargetId={}, status={}", dispatchTargetId, target.status)
			return
		}
		val template = target.template
		val token = latestToken(template.account.id)
		if (token == null) {
			log.warn("팔로우 확인 버튼 처리 불가: 유효한 액세스 토큰 없음 accountId={}", template.account.id)
			return
		}

		val followCheck = runCatching {
			val profile = instagramGraphClient.getUserProfile(token, senderPsid)
			val username = profile["username"] as? String
			val isFollowing = profile["is_user_follow_business"] as? Boolean ?: false
			username to isFollowing
		}
		followCheck.onFailure { ex ->
			handleSendFailure(target, DispatchStage.AUDIENCE_MESSAGE, null, ex) {
				sendLogRepository.save(
					SendLog(
						template = template,
						audienceType = null,
						recipientPlatformUserId = senderPsid,
						result = SendResult.FAILED,
						failureReason = ex.message,
					),
				)
			}
		}
		val (username, isFollowing) = followCheck.getOrNull() ?: return

		log.info(
			"팔로우 여부 확인 결과: dispatchTargetId={}, senderPsid={}, isFollowing={}, previousStatus={}",
			dispatchTargetId,
			senderPsid,
			isFollowing,
			target.status,
		)

		if (target.status == DispatchStatus.NON_FOLLOWER_SENT && !isFollowing) {
			log.info("팔로우 확인 재클릭: 여전히 팔로우하지 않음, 처리 생략 dispatchTargetId={}", dispatchTargetId)
			return
		}

		target.recordUsername(username)

		val audienceType = if (isFollowing) AudienceType.FOLLOWER else AudienceType.NON_FOLLOWER
		if (!hasSendCapacity(template.account.id)) {
			log.info("시간당 발송 한도 근접, 재시도 예약: dispatchTargetId={}", dispatchTargetId)
			target.markRetryPending(DispatchStage.AUDIENCE_MESSAGE, Instant.now().plus(RATE_LIMIT_DEFER_DELAY), audienceType, countsAsAttempt = false)
			return
		}

		runCatching {
			sendAndRecordAudienceMessages(target, template, audienceType, token, senderPsid, username)
		}.onFailure { ex ->
			handleSendFailure(target, DispatchStage.AUDIENCE_MESSAGE, audienceType, ex) {
				sendLogRepository.save(
					SendLog(
						template = template,
						audienceType = audienceType,
						recipientPlatformUserId = senderPsid,
						result = SendResult.FAILED,
						failureReason = ex.message,
					),
				)
			}
		}
	}

	// 팔로우 여부는 이미 확인된 상태로 재시도하는 경로. 재시도 때마다 Graph API를 다시 호출하지 않기 위해 분리했다.
	private fun resumeAudienceMessageSend(target: DispatchTarget, audienceType: AudienceType) {
		log.info("재시도: 분기 발송 재개 dispatchTargetId={}, audienceType={}", target.id, audienceType)
		val template = target.template
		val token = latestToken(template.account.id)
		if (token == null) {
			log.warn("분기 발송 재시도 불가: 유효한 액세스 토큰 없음 accountId={}", template.account.id)
			return
		}
		if (!hasSendCapacity(template.account.id)) {
			target.markRetryPending(DispatchStage.AUDIENCE_MESSAGE, Instant.now().plus(RATE_LIMIT_DEFER_DELAY), audienceType, countsAsAttempt = false)
			return
		}

		runCatching {
			sendAndRecordAudienceMessages(target, template, audienceType, token, target.recipientPlatformUserId, target.resolvedUsername)
		}.onFailure { ex ->
			handleSendFailure(target, DispatchStage.AUDIENCE_MESSAGE, audienceType, ex) {
				sendLogRepository.save(
					SendLog(
						template = template,
						audienceType = audienceType,
						recipientPlatformUserId = target.recipientPlatformUserId,
						result = SendResult.FAILED,
						failureReason = ex.message,
					),
				)
			}
		}
	}

	private fun sendAndRecordAudienceMessages(
		target: DispatchTarget,
		template: Template,
		audienceType: AudienceType,
		token: String,
		recipientId: String,
		username: String?,
	) {
		sendAudienceMessages(template, audienceType, token, recipientId, username)
		if (audienceType == AudienceType.FOLLOWER) target.markSent(Instant.now()) else target.markNonFollowerSent(Instant.now())
		sendLogRepository.save(
			SendLog(
				template = template,
				audienceType = audienceType,
				recipientPlatformUserId = recipientId,
				recipientUsername = username,
				result = SendResult.SUCCESS,
			),
		)
		log.info("분기 발송 완료: dispatchTargetId={}, audienceType={}", target.id, audienceType)
	}

	// 레이트리밋 등 일시적 오류면 지수 백오프 후 재시도를 예약하고, 그 외(잘못된 토큰/요청 등)나 재시도 횟수를 다 쓴 경우는 영구 실패 처리한다.
	private fun handleSendFailure(
		target: DispatchTarget,
		stage: DispatchStage,
		retryAudienceType: AudienceType?,
		ex: Throwable,
		onGiveUp: () -> Unit,
	) {
		if (instagramErrorClassifier.isRetryable(ex) && target.retryCount < dispatchProperties.maxRetryCount) {
			val delay = backoffDelay(target.retryCount + 1)
			log.warn(
				"발송 실패, 재시도 예약: dispatchTargetId={}, stage={}, retryCount={}, delayMinutes={}",
				target.id,
				stage,
				target.retryCount + 1,
				delay.toMinutes(),
				ex,
			)
			target.markRetryPending(stage, Instant.now().plus(delay), retryAudienceType)
		} else {
			log.warn("발송 최종 실패: dispatchTargetId={}, stage={}, retryCount={}", target.id, stage, target.retryCount, ex)
			target.markFailed(Instant.now())
			onGiveUp()
		}
	}

	// 최근 1시간 내 성공 발송 건수가 한도에 근접했으면 새 발송을 미룬다 (Meta 레이트리밋 사전 회피).
	private fun hasSendCapacity(accountId: Long): Boolean {
		val since = Instant.now().minus(Duration.ofHours(1))
		val recentSuccessCount = sendLogRepository.countByTemplateAccountIdAndResultAndCreatedAtAfter(accountId, SendResult.SUCCESS, since)
		return recentSuccessCount < dispatchProperties.hourlySendLimit
	}

	private fun backoffDelay(retryCount: Int): Duration {
		val minutes = (1L shl retryCount.coerceIn(1, 6)).coerceAtMost(60L)
		return Duration.ofMinutes(minutes)
	}

	private fun sendAudienceMessages(template: Template, audienceType: AudienceType, token: String, recipientId: String, username: String?) {
		val messages = template.messages.filter { it.audienceType == audienceType }.sortedBy { it.orderIndex }
		if (messages.isEmpty()) {
			log.warn("발송할 메시지가 없습니다: templateId={}, audienceType={}", template.id, audienceType)
		}
		messages.forEach { message ->
			instagramMessagingClient.sendDirectMessage(token, recipientId, MessagePayloadBuilder.fromTemplateMessage(message, username))
		}
	}

	private fun latestToken(accountId: Long): String? =
		accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(accountId, TokenRefreshStatus.SUCCESS)?.encryptedToken
}
