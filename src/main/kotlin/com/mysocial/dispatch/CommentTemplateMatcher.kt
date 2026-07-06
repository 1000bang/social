package com.mysocial.dispatch

import com.mysocial.template.TemplateRepository
import org.springframework.stereotype.Service

@Service
class CommentTemplateMatcher(
	private val templateRepository: TemplateRepository,
	private val dispatchTargetEnqueuer: DispatchTargetEnqueuer,
) {
	fun match(postId: Long, commentPlatformId: String, commenterPlatformUserId: String, commentText: String) {
		templateRepository.findByPostId(postId)
			.filter { it.matchesKeyword(commentText) }
			.forEach { template ->
				dispatchTargetEnqueuer.enqueue(template, TriggerType.COMMENT, commentPlatformId, commenterPlatformUserId)
			}
	}
}
