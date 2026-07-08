package com.mysocial.dispatch

import com.mysocial.template.TemplateRepository
import org.springframework.stereotype.Service

@Service
class CommentTemplateMatcher(
	private val templateRepository: TemplateRepository,
	private val dispatchTargetEnqueuer: DispatchTargetEnqueuer,
	private val dispatchExecutor: DispatchExecutor,
) {
	fun match(postId: Long, commentPlatformId: String, commenterPlatformUserId: String, commentText: String) {
		templateRepository.findByPostId(postId).forEach { template ->
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
}
