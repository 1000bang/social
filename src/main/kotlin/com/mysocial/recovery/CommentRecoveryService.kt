package com.mysocial.recovery

import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.comment.Comment
import com.mysocial.comment.CommentRepository
import com.mysocial.dispatch.CommentTemplateMatcher
import com.mysocial.instagram.InstagramCommentItem
import com.mysocial.instagram.InstagramGraphClient
import com.mysocial.template.Template
import com.mysocial.template.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class CommentRecoveryService(
	private val templateRepository: TemplateRepository,
	private val commentRepository: CommentRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val instagramGraphClient: InstagramGraphClient,
	private val commentTemplateMatcher: CommentTemplateMatcher,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional(readOnly = true)
	fun listRecoveryCards(accountId: Long): List<RecoveryCardResponse> {
		val token = latestToken(accountId) ?: return emptyList()
		// 사용 중지된 템플릿은 의도적으로 꺼둔 것이므로 다시 사용으로 전환하기 전까지는 카드에 노출하지 않는다.
		val templates = templateRepository.findAllByAccountId(accountId).filter { it.activeYn }

		return templates.mapNotNull { template ->
			val post = template.post
			val comments = fetchUnrepliedComments(token, post.platformPostId, template.account.platformAccountId)
			if (comments.isEmpty()) return@mapNotNull null

			val thumbnailUrl = runCatching { instagramGraphClient.getMediaThumbnail(token, post.platformPostId) }.getOrNull()

			RecoveryCardResponse(
				postId = post.id,
				templateId = template.id,
				templateName = template.name,
				thumbnailUrl = thumbnailUrl,
				comments = comments
					.sortedByDescending { parseTimestamp(it.timestamp) ?: Instant.EPOCH }
					.mapNotNull { item ->
						val ts = parseTimestamp(item.timestamp) ?: return@mapNotNull null
						RecoveryCommentResponse(
							commentId = item.id,
							authorUsername = item.from?.username,
							text = item.text ?: "",
							timestamp = ts,
						)
					},
			)
		}
	}

	@Transactional
	fun processComment(accountId: Long, postId: Long, commentId: String) {
		val template = templateForPost(accountId, postId)
		if (commentRepository.existsByPlatformCommentId(commentId)) {
			log.info("복구 처리 생략: 이미 처리된 댓글 commentId={}", commentId)
			return
		}

		val token = latestToken(accountId) ?: throw IllegalArgumentException("유효한 액세스 토큰이 없습니다")
		val detail = instagramGraphClient.getComment(token, commentId)
			?: throw IllegalArgumentException("댓글을 찾을 수 없습니다: $commentId")
		val fromId = detail.from?.id
			?: throw IllegalArgumentException("댓글 작성자 정보를 찾을 수 없습니다: $commentId")

		if (fromId == template.account.platformAccountId) {
			log.info("복구 처리 생략: 비즈니스 계정 자신이 남긴 댓글 commentId={}", commentId)
			return
		}
		if (detail.replies?.data?.any { it.from?.id == template.account.platformAccountId } == true) {
			log.info("복구 처리 생략: 이미 답글을 남긴 댓글 commentId={}", commentId)
			return
		}

		saveAndMatch(template, commentId, fromId, detail.from.username, detail.text ?: "", parseTimestamp(detail.timestamp))
	}

	@Transactional
	fun processAllForPost(accountId: Long, postId: Long) {
		val template = templateForPost(accountId, postId)
		val token = latestToken(accountId) ?: throw IllegalArgumentException("유효한 액세스 토큰이 없습니다")

		val comments = fetchUnrepliedComments(token, template.post.platformPostId, template.account.platformAccountId)
			.sortedBy { parseTimestamp(it.timestamp) ?: Instant.EPOCH }

		log.info("일괄 복구 처리 시작: templateId={}, count={}", template.id, comments.size)
		comments.forEach { item ->
			val fromId = item.from?.id ?: return@forEach
			if (commentRepository.existsByPlatformCommentId(item.id)) return@forEach
			saveAndMatch(template, item.id, fromId, item.from.username, item.text ?: "", parseTimestamp(item.timestamp))
		}
	}

	private fun saveAndMatch(
		template: Template,
		commentId: String,
		fromId: String,
		fromUsername: String?,
		text: String,
		publishedAt: Instant?,
	) {
		commentRepository.save(
			Comment(
				post = template.post,
				platformCommentId = commentId,
				authorUsername = fromUsername,
				text = text,
				publishedAt = publishedAt ?: Instant.now(),
			),
		)
		log.info("복구 처리: templateId={}, commentId={}", template.id, commentId)
		commentTemplateMatcher.match(template.post.id, commentId, fromId, text)
	}

	private fun templateForPost(accountId: Long, postId: Long): Template {
		val template = templateRepository.findByPostId(postId).firstOrNull()
			?: throw IllegalArgumentException("게시물에 연결된 템플릿을 찾을 수 없습니다: $postId")
		require(template.account.id == accountId) { "게시물을 찾을 수 없습니다: $postId" }
		return template
	}

	// 게시물의 댓글을 페이지 끝까지 훑어, 비즈니스 계정이 아직 답글을 남기지 않은 댓글만 모은다.
	// Graph API에 "답글 없는 댓글만" 서버 필터가 없어 전체를 훑어야 하므로, MAX_PAGES로 최악의 경우를 방어한다.
	private fun fetchUnrepliedComments(token: String, mediaId: String, businessAccountId: String): List<InstagramCommentItem> {
		val result = mutableListOf<InstagramCommentItem>()
		var after: String? = null
		var page = 0

		while (page < MAX_PAGES) {
			val response = instagramGraphClient.listComments(token, mediaId, after)
			if (response.data.isEmpty()) break

			for (item in response.data) {
				val alreadyReplied = item.replies?.data?.any { it.from?.id == businessAccountId } ?: false
				if (item.from?.id != businessAccountId && !alreadyReplied) {
					result.add(item)
				}
			}

			after = response.paging?.cursors?.after ?: break
			page++
		}
		return result
	}

	private fun latestToken(accountId: Long): String? =
		accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(accountId, TokenRefreshStatus.SUCCESS)?.encryptedToken

	private fun parseTimestamp(value: String?): Instant? {
		if (value == null) return null
		return runCatching { Instant.from(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").parse(value)) }.getOrNull()
	}

	companion object {
		private const val MAX_PAGES = 10
	}
}
