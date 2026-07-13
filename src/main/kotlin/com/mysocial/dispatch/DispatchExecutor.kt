package com.mysocial.dispatch

import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.instagram.InstagramGraphClient
import com.mysocial.instagram.InstagramMessagingClient
import com.mysocial.template.AudienceType
import com.mysocial.template.Template
import com.mysocial.template.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private const val FOLLOW_PROMPT_TEXT = "댓글을 남겨주셔서 감사합니다. 저를 팔로우해주셨다면 아래 버튼을 클릭해주세요!"

@Service
class DispatchExecutor(
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val templateRepository: TemplateRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val sendLogRepository: SendLogRepository,
	private val instagramGraphClient: InstagramGraphClient,
	private val instagramMessagingClient: InstagramMessagingClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun replyToNonMatchingComment(templateId: Long, commentId: String) {
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
			instagramMessagingClient.replyToComment(token, commentId, template.resolvedNonKeywordCommentReplyText())
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
		if (target.status != DispatchStatus.PENDING) {
			log.warn("초기 발송 처리 불가: 대기(PENDING) 상태가 아님 dispatchTargetId={}, status={}", dispatchTargetId, target.status)
			return
		}
		val template = target.template
		val token = latestToken(template.account.id)
		if (token == null) {
			log.warn("초기 발송 처리 불가: 유효한 액세스 토큰 없음 accountId={}", template.account.id)
			return
		}

		runCatching {
			if (target.triggerType == TriggerType.COMMENT) {
				instagramMessagingClient.replyToComment(token, target.platformTriggerId, template.resolvedCommentReplyText())
				instagramMessagingClient.sendPrivateReply(
					token,
					target.platformTriggerId,
					MessagePayloadBuilder.promptWithFollowButton(FOLLOW_PROMPT_TEXT, target.id),
				)
			} else {
				instagramMessagingClient.sendDirectMessage(
					token,
					target.recipientPlatformUserId,
					MessagePayloadBuilder.promptWithFollowButton(FOLLOW_PROMPT_TEXT, target.id),
				)
			}
			target.markAwaitingFollowCheck()
		}.onSuccess {
			log.info("초기 발송 완료: dispatchTargetId={}, status={}", dispatchTargetId, target.status)
		}.onFailure { ex ->
			log.warn("초기 발송 실패: dispatchTargetId={}", dispatchTargetId, ex)
			target.markFailed(Instant.now())
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

	@Transactional
	fun handleFollowCheckClick(dispatchTargetId: Long, senderPsid: String) {
		log.info("팔로우 확인 버튼 처리 진입: dispatchTargetId={}, senderPsid={}", dispatchTargetId, senderPsid)
		val target = dispatchTargetRepository.findById(dispatchTargetId).orElse(null)
		if (target == null) {
			log.warn("팔로우 확인 버튼 처리 불가: dispatchTarget을 찾을 수 없음 dispatchTargetId={}", dispatchTargetId)
			return
		}
		// 논팔로워로 처리된 뒤에도 버튼을 다시 누르면 팔로우 여부를 재확인해서, 그사이 팔로우했다면 팔로워 메시지를 마저 보낸다.
		if (target.status != DispatchStatus.AWAITING_FOLLOW_CHECK && target.status != DispatchStatus.NON_FOLLOWER_SENT) {
			log.warn("팔로우 확인 버튼 처리 불가: 대기 상태가 아님 dispatchTargetId={}, status={}", dispatchTargetId, target.status)
			return
		}
		val template = target.template
		val token = latestToken(template.account.id)
		if (token == null) {
			log.warn("팔로우 확인 버튼 처리 불가: 유효한 액세스 토큰 없음 accountId={}", template.account.id)
			return
		}

		runCatching {
			val profile = instagramGraphClient.getUserProfile(token, senderPsid)
			val username = profile["username"] as? String
			val isFollowing = profile["is_user_follow_business"] as? Boolean ?: false
			log.info(
				"팔로우 여부 확인 결과: dispatchTargetId={}, senderPsid={}, isFollowing={}, previousStatus={}",
				dispatchTargetId,
				senderPsid,
				isFollowing,
				target.status,
			)

			if (target.status == DispatchStatus.NON_FOLLOWER_SENT && !isFollowing) {
				log.info("팔로우 확인 재클릭: 여전히 팔로우하지 않음, 처리 생략 dispatchTargetId={}", dispatchTargetId)
				return@runCatching
			}

			val audienceType = if (isFollowing) AudienceType.FOLLOWER else AudienceType.NON_FOLLOWER
			sendAudienceMessages(template, audienceType, token, senderPsid)

			if (audienceType == AudienceType.FOLLOWER) {
				target.markSent(Instant.now())
			} else {
				target.markNonFollowerSent(Instant.now())
			}
			sendLogRepository.save(
				SendLog(
					template = template,
					audienceType = audienceType,
					recipientPlatformUserId = senderPsid,
					recipientUsername = username,
					result = SendResult.SUCCESS,
				),
			)
		}.onSuccess {
			log.info("팔로우 분기 발송 완료: dispatchTargetId={}", dispatchTargetId)
		}.onFailure { ex ->
			log.warn("팔로우 분기 발송 실패: dispatchTargetId={}", dispatchTargetId, ex)
			target.markFailed(Instant.now())
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

	private fun sendAudienceMessages(template: Template, audienceType: AudienceType, token: String, recipientId: String) {
		val messages = template.messages.filter { it.audienceType == audienceType }.sortedBy { it.orderIndex }
		if (messages.isEmpty()) {
			log.warn("발송할 메시지가 없습니다: templateId={}, audienceType={}", template.id, audienceType)
		}
		messages.forEach { message ->
			instagramMessagingClient.sendDirectMessage(token, recipientId, MessagePayloadBuilder.fromTemplateMessage(message))
		}
	}

	private fun latestToken(accountId: Long): String? =
		accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(accountId, TokenRefreshStatus.SUCCESS)?.encryptedToken
}
