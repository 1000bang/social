package com.mysocial.template

import java.time.Instant
import java.time.LocalTime

data class CreateTemplateRequest(
	val name: String,
	val postId: Long,
	val dispatchTime: LocalTime? = null,
	val keywords: List<String> = emptyList(),
	val dmKeyword: String? = null,
	val commentReplyText: String? = null,
	val nonKeywordCommentReplyText: String? = null,
	val nonKeywordReplyEnabled: Boolean = true,
	val followerMessages: List<MessageInput> = emptyList(),
	val nonFollowerMessages: List<MessageInput> = emptyList(),
)

data class MessageInput(
	val messageType: MessageType,
	val textContent: String? = null,
	val imageUrl: String? = null,
	val carouselItems: List<CarouselItemInput> = emptyList(),
)

data class CarouselItemInput(
	val imageUrl: String,
	val title: String? = null,
	val subtitle: String? = null,
	val buttonText: String? = null,
	val buttonUrl: String? = null,
)

data class TemplateResponse(
	val id: Long,
	val name: String,
	val postId: Long,
	val dispatchTime: LocalTime?,
	val keywords: List<String>,
	val dmKeyword: String?,
	val commentReplyText: String?,
	val nonKeywordCommentReplyText: String?,
	val nonKeywordReplyEnabled: Boolean,
	val createdAt: Instant,
) {
	companion object {
		fun from(template: Template): TemplateResponse = TemplateResponse(
			id = template.id,
			name = template.name,
			postId = template.post.id,
			dispatchTime = template.dispatchTime,
			keywords = template.keywords.map { it.keyword },
			dmKeyword = template.dmKeyword,
			commentReplyText = template.commentReplyText,
			nonKeywordCommentReplyText = template.nonKeywordCommentReplyText,
			nonKeywordReplyEnabled = template.nonKeywordReplyEnabled,
			createdAt = template.createdAt,
		)
	}
}

data class TemplateDetailResponse(
	val id: Long,
	val name: String,
	val postId: Long,
	val dispatchTime: LocalTime?,
	val keywords: List<String>,
	val dmKeyword: String?,
	val commentReplyText: String?,
	val nonKeywordCommentReplyText: String?,
	val nonKeywordReplyEnabled: Boolean,
	val followerMessages: List<MessageInput>,
	val nonFollowerMessages: List<MessageInput>,
	val createdAt: Instant,
) {
	companion object {
		fun from(template: Template): TemplateDetailResponse = TemplateDetailResponse(
			id = template.id,
			name = template.name,
			postId = template.post.id,
			dispatchTime = template.dispatchTime,
			keywords = template.keywords.map { it.keyword },
			dmKeyword = template.dmKeyword,
			commentReplyText = template.commentReplyText,
			nonKeywordCommentReplyText = template.nonKeywordCommentReplyText,
			nonKeywordReplyEnabled = template.nonKeywordReplyEnabled,
			followerMessages = toMessageInputs(template, AudienceType.FOLLOWER),
			nonFollowerMessages = toMessageInputs(template, AudienceType.NON_FOLLOWER),
			createdAt = template.createdAt,
		)

		private fun toMessageInputs(template: Template, audienceType: AudienceType): List<MessageInput> =
			template.messages
				.filter { it.audienceType == audienceType }
				.sortedBy { it.orderIndex }
				.map { message ->
					MessageInput(
						messageType = message.messageType,
						textContent = message.textContent,
						imageUrl = message.imageUrl,
						carouselItems = message.carouselItems.sortedBy { it.orderIndex }.map { item ->
							CarouselItemInput(
								imageUrl = item.imageUrl,
								title = item.title,
								subtitle = item.subtitle,
								buttonText = item.buttonText,
								buttonUrl = item.buttonUrl,
							)
						},
					)
				}
	}
}
