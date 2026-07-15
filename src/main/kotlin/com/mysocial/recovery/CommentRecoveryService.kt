package com.mysocial.recovery

import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.AccountRepository
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
	private val accountRepository: AccountRepository,
	private val templateRepository: TemplateRepository,
	private val commentRepository: CommentRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val recoveryCheckpointRepository: RecoveryCheckpointRepository,
	private val unprocessedCommentRepository: UnprocessedCommentRepository,
	private val instagramGraphClient: InstagramGraphClient,
	private val commentTemplateMatcher: CommentTemplateMatcher,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Transactional(readOnly = true)
	fun listRecoveryCards(accountId: Long): List<RecoveryCardResponse> {
		val token = latestToken(accountId) ?: return emptyList()
		// 사용 중지된 템플릿은 의도적으로 꺼둔 것이므로 다시 사용으로 전환하기 전까지는 카드에 노출하지 않는다.
		val templates = templateRepository.findAllByAccountId(accountId).filter { it.activeYn }
		val checkpoint = recoveryCheckpointRepository.findByAccountId(accountId)?.lastCheckedAt

		return templates.mapNotNull { template ->
			val archived = unprocessedCommentRepository.findByTemplateId(template.id)
			val archivedIds = archived.map { it.platformCommentId }.toSet()

			val live = if (checkpoint != null) {
				fetchUnrepliedComments(token, template.post.platformPostId, template, checkpoint)
					.filter { it.id !in archivedIds }
			} else {
				emptyList()
			}

			val comments = archived.map {
				RecoveryCommentResponse(
					commentId = it.platformCommentId,
					authorUsername = it.authorUsername,
					text = it.text,
					timestamp = it.publishedAt,
				)
			} + live.mapNotNull { item ->
				val ts = parseTimestamp(item.timestamp) ?: return@mapNotNull null
				RecoveryCommentResponse(
					commentId = item.id,
					authorUsername = item.from?.username,
					text = item.text ?: "",
					timestamp = ts,
				)
			}
			if (comments.isEmpty()) return@mapNotNull null

			val thumbnailUrl = runCatching { instagramGraphClient.getMediaThumbnail(token, template.post.platformPostId) }.getOrNull()

			RecoveryCardResponse(
				postId = template.post.id,
				templateId = template.id,
				templateName = template.name,
				thumbnailUrl = thumbnailUrl,
				comments = comments.sortedByDescending { it.timestamp },
			)
		}
	}

	@Transactional
	fun processComment(accountId: Long, postId: Long, commentId: String) {
		val template = templateForPost(accountId, postId)
		if (commentRepository.existsByPlatformCommentId(commentId)) {
			log.info("복구 처리 생략: 이미 처리된 댓글 commentId={}", commentId)
			unprocessedCommentRepository.deleteByTemplateIdAndPlatformCommentId(template.id, commentId)
			return
		}

		val archived = unprocessedCommentRepository.findByTemplateId(template.id).find { it.platformCommentId == commentId }
		if (archived != null) {
			saveAndMatch(template, archived.platformCommentId, archived.authorPlatformUserId, archived.authorUsername, archived.text, archived.publishedAt)
			unprocessedCommentRepository.delete(archived)
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
		if (hasBusinessReply(token, commentId, template)) {
			log.info("복구 처리 생략: 이미 답글을 남긴 댓글 commentId={}", commentId)
			return
		}

		saveAndMatch(template, commentId, fromId, detail.from.username, detail.text ?: "", parseTimestamp(detail.timestamp))
	}

	@Transactional
	fun processAllForPost(accountId: Long, postId: Long) {
		val template = templateForPost(accountId, postId)
		val token = latestToken(accountId) ?: throw IllegalArgumentException("유효한 액세스 토큰이 없습니다")

		val archived = unprocessedCommentRepository.findByTemplateId(template.id).sortedBy { it.publishedAt }
		log.info("일괄 복구 처리 시작(저장된 백로그): templateId={}, count={}", template.id, archived.size)
		archived.forEach { item ->
			if (!commentRepository.existsByPlatformCommentId(item.platformCommentId)) {
				saveAndMatch(template, item.platformCommentId, item.authorPlatformUserId, item.authorUsername, item.text, item.publishedAt)
			}
			unprocessedCommentRepository.delete(item)
		}

		val checkpoint = recoveryCheckpointRepository.findByAccountId(accountId)?.lastCheckedAt
		if (checkpoint != null) {
			val liveComments = fetchUnrepliedComments(token, template.post.platformPostId, template, checkpoint)
				.sortedBy { parseTimestamp(it.timestamp) ?: Instant.EPOCH }
			log.info("일괄 복구 처리 시작(실시간 구간): templateId={}, count={}", template.id, liveComments.size)
			liveComments.forEach { item ->
				val fromId = item.from?.id ?: return@forEach
				if (commentRepository.existsByPlatformCommentId(item.id)) return@forEach
				saveAndMatch(template, item.id, fromId, item.from.username, item.text ?: "", parseTimestamp(item.timestamp))
			}
		}
	}

	// 체크포인트~지금 사이의 미처리 댓글을 찾아 테이블에 쌓아두고, 체크포인트를 지금 시각으로 전진시킨다.
	// 매일 새벽 스케줄에서만 호출되며, 여기서 저장된 항목은 사용자가 처리하기 전까지 지워지지 않는다.
	@Transactional
	fun archiveUnprocessedComments(accountId: Long) {
		val token = latestToken(accountId) ?: return
		val checkpoint = recoveryCheckpointRepository.findByAccountId(accountId)
			?: RecoveryCheckpoint(account = accountRepository.getReferenceById(accountId), lastCheckedAt = Instant.now())
		val since = checkpoint.lastCheckedAt
		val now = Instant.now()

		val templates = templateRepository.findAllByAccountId(accountId).filter { it.activeYn }
		templates.forEach { template ->
			val comments = fetchUnrepliedComments(token, template.post.platformPostId, template, since)
			comments.forEach { item ->
				val ts = parseTimestamp(item.timestamp) ?: return@forEach
				val fromId = item.from?.id ?: return@forEach
				if (!unprocessedCommentRepository.existsByTemplateIdAndPlatformCommentId(template.id, item.id)) {
					unprocessedCommentRepository.save(
						UnprocessedComment(
							template = template,
							platformCommentId = item.id,
							authorPlatformUserId = fromId,
							authorUsername = item.from.username,
							text = item.text ?: "",
							publishedAt = ts,
						),
					)
				}
			}
		}

		checkpoint.lastCheckedAt = now
		recoveryCheckpointRepository.save(checkpoint)
		log.info("미처리 댓글 아카이빙 완료: accountId={}, since={}, until={}", accountId, since, now)
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

	// since보다 최신인 댓글만 최신순으로 페이지를 넘겨가며 수집하고, 그중 비즈니스 계정이 아직 답글을 남기지 않은 것만 남긴다.
	// since보다 오래된 댓글에 도달하면 그 이전은 이미 확인된 구간이므로 중단한다. (MAX_PAGES는 최악의 경우를 방어)
	private fun fetchUnrepliedComments(token: String, mediaId: String, template: Template, since: Instant): List<InstagramCommentItem> {
		val businessAccountId = template.account.platformAccountId
		val result = mutableListOf<InstagramCommentItem>()
		var after: String? = null
		var page = 0

		while (page < MAX_PAGES) {
			val response = instagramGraphClient.listComments(token, mediaId, after)
			if (response.data.isEmpty()) break

			var reachedBoundary = false
			for (item in response.data) {
				val ts = parseTimestamp(item.timestamp)
				if (ts == null || !ts.isAfter(since)) {
					reachedBoundary = true
					break
				}
				if (item.from?.id == businessAccountId) continue
				if (hasBusinessReply(token, item.id, template)) continue
				result.add(item)
			}
			if (reachedBoundary) break

			after = response.paging?.cursors?.after ?: break
			page++
		}
		return result
	}

	// 이 계정/토큰으로는 Graph API가 답글의 from을 아예 내려주지 않아서(권한/API 자체의 한계로 보임),
	// 답글 작성자 비교 대신 답글 텍스트가 템플릿에 설정된 우리 봇 문구와 일치하는지로 "이미 답글 남김"을 판단한다.
	private fun hasBusinessReply(token: String, commentId: String, template: Template): Boolean {
		val replies = runCatching { instagramGraphClient.listReplies(token, commentId) }.getOrNull() ?: return false
		val knownReplyTexts = setOf(template.resolvedCommentReplyText(), template.resolvedNonKeywordCommentReplyText())
		return replies.data.any { it.text in knownReplyTexts }
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
