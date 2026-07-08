package com.mysocial.dispatch

import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.instagram.InstagramGraphClient
import com.mysocial.instagram.InstagramMessagingClient
import com.mysocial.template.AudienceType
import com.mysocial.template.Template
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

private const val FOLLOW_PROMPT_TEXT = "댓글을 남겨주셔서 감사합니다. 저를 팔로우해주셨다면 아래 버튼을 클릭해주세요!"

@Service
class DispatchExecutor(
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val sendLogRepository: SendLogRepository,
	private val instagramGraphClient: InstagramGraphClient,
	private val instagramMessagingClient: InstagramMessagingClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional
	fun sendInitialPrompt(dispatchTargetId: Long) {
		val target = dispatchTargetRepository.findById(dispatchTargetId).orElse(null) ?: return
		if (target.status != DispatchStatus.PENDING) return
		val template = target.template
		val token = latestToken(template.account.id) ?: return

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
		val target = dispatchTargetRepository.findById(dispatchTargetId).orElse(null) ?: return
		if (target.status != DispatchStatus.AWAITING_FOLLOW_CHECK) return
		val template = target.template
		val token = latestToken(template.account.id) ?: return

		runCatching {
			val profile = instagramGraphClient.getUserProfile(token, senderPsid)
			val isFollowing = profile["is_user_follow_business"] as? Boolean ?: false
			val audienceType = if (isFollowing) AudienceType.FOLLOWER else AudienceType.NON_FOLLOWER

			sendAudienceMessages(template, audienceType, token, senderPsid)

			target.markSent(Instant.now())
			sendLogRepository.save(
				SendLog(
					template = template,
					audienceType = audienceType,
					recipientPlatformUserId = senderPsid,
					result = SendResult.SUCCESS,
				),
			)
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
		template.messages
			.filter { it.audienceType == audienceType }
			.sortedBy { it.orderIndex }
			.forEach { message ->
				instagramMessagingClient.sendDirectMessage(token, recipientId, MessagePayloadBuilder.fromTemplateMessage(message))
			}
	}

	private fun latestToken(accountId: Long): String? =
		accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(accountId, TokenRefreshStatus.SUCCESS)?.encryptedToken
}
