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
		val event = webhookEventRepository.findById(webhookEventId).orElse(null)
		if (event == null) {
			log.warn("웹훅 이벤트 처리 불가: webhookEventId={}를 찾을 수 없음", webhookEventId)
			return
		}

		log.info("웹훅 이벤트 처리 시작: webhookEventId={}, eventId={}", event.id, event.eventId)

		runCatching {
			val payload = objectMapper.readValue(event.payload, InstagramWebhookPayload::class.java)
			log.info("웹훅 payload 파싱 완료: webhookEventId={}, entryCount={}", event.id, payload.entry.size)
			payload.entry.forEach(::handleEntry)
		}.onFailure { log.warn("웹훅 payload 처리 실패: eventId={}", event.eventId, it) }

		event.markProcessed(Instant.now())
		webhookEventRepository.save(event)
		log.info("웹훅 이벤트 처리 완료: webhookEventId={}", event.id)
	}

	private fun handleEntry(entry: WebhookEntry) {
		val account = accountRepository.findByPlatformAndPlatformAccountId(SocialPlatform.INSTAGRAM, entry.id)
		if (account == null) {
			log.warn("웹훅 entry 처리 불가: 등록된 계정을 찾을 수 없음 platformAccountId={}", entry.id)
			return
		}

		val commentChanges = entry.changes.filter { it.field == "comments" }.mapNotNull { it.value }
		log.info(
			"웹훅 entry 처리: accountId={}, platformAccountId={}, commentChanges={}, messagingEvents={}",
			account.id,
			entry.id,
			commentChanges.size,
			entry.messaging.size,
		)

		commentChanges.forEach { handleComment(account, it) }
		entry.messaging.forEach { handleMessage(account, it) }
	}

	private fun handleComment(account: Account, value: CommentChangeValue) {
		val mediaId = value.media?.id
		val from = value.from
		val text = value.text
		log.info(
			"댓글 웹훅 수신: commentId={}, mediaId={}, fromId={}, fromUsername={}, text={}",
			value.id,
			mediaId,
			from?.id,
			from?.username,
			text,
		)

		if (mediaId == null || from == null || text == null) {
			log.warn("댓글 웹훅 처리 불가: 필수 필드 누락 commentId={}", value.id)
			return
		}

		// 비즈니스 계정이 자동 답글로 남긴 댓글이 다시 웹훅으로 들어와 무한 답글 루프에 빠지는 것을 방지
		if (from.id == account.platformAccountId) {
			log.info("댓글 웹훅 무시: 비즈니스 계정 자신이 남긴 댓글(자동 답글 에코) commentId={}", value.id)
			return
		}

		if (commentRepository.existsByPlatformCommentId(value.id)) {
			log.info("댓글 웹훅 무시: 이미 처리된 댓글 commentId={}", value.id)
			return
		}

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

		log.info("댓글 템플릿 매칭 시작: postId={}, commentId={}, fromId={}", post.id, value.id, from.id)
		commentTemplateMatcher.match(post.id, value.id, from.id, text)
	}

	private fun handleMessage(account: Account, event: WebhookMessagingEvent) {
		val senderId = event.sender?.id
		log.info(
			"메시징 웹훅 수신: senderId={}, hasPostback={}, hasMessage={}, isEcho={}",
			senderId,
			event.postback != null,
			event.message != null,
			event.message?.isEcho,
		)

		if (senderId == null) {
			log.warn("메시징 웹훅 처리 불가: senderId 없음")
			return
		}

		val postbackPayload = event.postback?.payload
		if (postbackPayload != null && postbackPayload.startsWith(FOLLOW_CHECK_PAYLOAD_PREFIX)) {
			val dispatchTargetId = postbackPayload.removePrefix(FOLLOW_CHECK_PAYLOAD_PREFIX).toLongOrNull()
			log.info("팔로우 확인 postback 수신: senderId={}, dispatchTargetId={}", senderId, dispatchTargetId)
			if (dispatchTargetId != null) {
				dispatchExecutor.handleFollowCheckClick(dispatchTargetId, senderId)
			} else {
				log.warn("팔로우 확인 postback payload 파싱 실패: payload={}", postbackPayload)
			}
			return
		}

		val mid = event.message?.mid
		if (mid == null) {
			log.info("메시징 웹훅 무시: message 필드 없음 (postback도 아님) senderId={}", senderId)
			return
		}
		if (event.message.isEcho) {
			log.info("메시징 웹훅 무시: 자신이 보낸 메시지 에코 mid={}", mid)
			return
		}
		val text = event.message.text
		if (text == null) {
			log.info("메시징 웹훅 무시: 텍스트 없는 메시지 mid={}", mid)
			return
		}

		if (directMessageRepository.existsByPlatformMessageId(mid)) {
			log.info("메시징 웹훅 무시: 이미 처리된 메시지 mid={}", mid)
			return
		}

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

		log.info("DM 키워드 매칭 시작: accountId={}, senderId={}, text={}", account.id, senderId, text)
		dmKeywordMatcher.match(account.id, mid, senderId, text)
	}
}
