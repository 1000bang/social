package com.mysocial.dispatch

import com.mysocial.template.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommentTemplateMatcher(
	private val templateRepository: TemplateRepository,
	private val dispatchTargetEnqueuer: DispatchTargetEnqueuer,
	private val dispatchExecutor: DispatchExecutor,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun match(postId: Long, commentPlatformId: String, commenterPlatformUserId: String, commentText: String) {
		val templates = templateRepository.findByPostId(postId)
		if (templates.isEmpty()) return

		// 게시물당 템플릿은 1개만 허용되지만, 그 제약이 생기기 전에 만들어진 중복 데이터가 남아있을 수 있어 방어적으로 최신 템플릿 하나만 사용한다.
		val template = templates.maxBy { it.createdAt }
		if (templates.size > 1) {
			log.warn(
				"게시물당 템플릿이 여러 개 존재합니다. 가장 최근 템플릿(id={})만 사용합니다: postId={}, templateIds={}",
				template.id,
				postId,
				templates.map { it.id },
			)
		}

		if (template.matchesKeyword(commentText)) {
			val target = dispatchTargetEnqueuer.enqueue(template, TriggerType.COMMENT, commentPlatformId, commenterPlatformUserId)
			if (target != null && template.dispatchTime == null) {
				dispatchExecutor.sendInitialPrompt(target.id)
			}
		} else {
			dispatchExecutor.replyToNonMatchingComment(template.id, commentPlatformId)
		}
	}
}
