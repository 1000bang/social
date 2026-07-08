package com.mysocial.webhook

import com.fasterxml.jackson.databind.ObjectMapper
import com.mysocial.account.Account
import com.mysocial.account.AccountRepository
import com.mysocial.comment.Comment
import com.mysocial.comment.CommentRepository
import com.mysocial.common.SocialPlatform
import com.mysocial.dispatch.CommentTemplateMatcher
import com.mysocial.dispatch.DispatchExecutor
import com.mysocial.dispatch.DmKeywordMatcher
import com.mysocial.dispatch.FOLLOW_CHECK_PAYLOAD_PREFIX
import com.mysocial.message.DirectMessage
import com.mysocial.message.DirectMessageRepository
import com.mysocial.message.DmThread
import com.mysocial.message.DmThreadRepository
import com.mysocial.message.MessageDirection
import com.mysocial.post.Post
import com.mysocial.post.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class WebhookEventProcessor(
	private val webhookEventRepository: WebhookEventRepository,
	private val objectMapper: ObjectMapper,
	private val accountRepository: AccountRepository,
	private val postRepository: PostRepository,
	private val commentRepository: CommentRepository,
	private val dmThreadRepository: DmThreadRepository,
	private val directMessageRepository: DirectMessageRepository,
	private val commentTemplateMatcher: CommentTemplateMatcher,
	private val dmKeywordMatcher: DmKeywordMatcher,
	private val dispatchExecutor: DispatchExecutor,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Async
	@Transactional
	fun process(webhookEventId: Long) {
		val event = webhookEventRepository.findById(webhookEventId).orElse(null) ?: return

		runCatching {
			val payload = objectMapper.readValue(event.payload, InstagramWebhookPayload::class.java)
			payload.entry.forEach(::handleEntry)
		}.onFailure { log.warn("웹훅 payload 처리 실패: eventId={}", event.eventId, it) }

		event.markProcessed(Instant.now())
		webhookEventRepository.save(event)
	}

	private fun handleEntry(entry: WebhookEntry) {
		val account = accountRepository.findByPlatformAndPlatformAccountId(SocialPlatform.INSTAGRAM, entry.id) ?: return

		entry.changes
			.filter { it.field == "comments" }
			.mapNotNull { it.value }
			.forEach { handleComment(account, it) }

		entry.messaging.forEach { handleMessage(account, it) }
	}

	private fun handleComment(account: Account, value: CommentChangeValue) {
		val mediaId = value.media?.id ?: return
		val from = value.from ?: return
		val text = value.text ?: return

		if (commentRepository.existsByPlatformCommentId(value.id)) return

		val post = postRepository.findByAccountIdAndPlatformPostId(account.id, mediaId)
			?: postRepository.save(Post(account = account, platformPostId = mediaId))

		commentRepository.save(
			Comment(
				post = post,
				platformCommentId = value.id,
				authorUsername = from.username,
				text = text,
				publishedAt = Instant.now(),
			),
		)

		commentTemplateMatcher.match(post.id, value.id, from.id, text)
	}

	private fun handleMessage(account: Account, event: WebhookMessagingEvent) {
		val senderId = event.sender?.id ?: return

		val postbackPayload = event.postback?.payload
		if (postbackPayload != null && postbackPayload.startsWith(FOLLOW_CHECK_PAYLOAD_PREFIX)) {
			val dispatchTargetId = postbackPayload.removePrefix(FOLLOW_CHECK_PAYLOAD_PREFIX).toLongOrNull()
			if (dispatchTargetId != null) {
				dispatchExecutor.handleFollowCheckClick(dispatchTargetId, senderId)
			}
			return
		}

		val mid = event.message?.mid ?: return
		val text = event.message.text ?: return

		if (directMessageRepository.existsByPlatformMessageId(mid)) return

		val thread = dmThreadRepository.findByAccountIdAndPlatformThreadId(account.id, senderId)
			?: dmThreadRepository.save(DmThread(account = account, platformThreadId = senderId))

		directMessageRepository.save(
			DirectMessage(
				thread = thread,
				platformMessageId = mid,
				direction = MessageDirection.INBOUND,
				text = text,
				sentAt = Instant.now(),
			),
		)

		dmKeywordMatcher.match(account.id, mid, senderId, text)
	}
}
