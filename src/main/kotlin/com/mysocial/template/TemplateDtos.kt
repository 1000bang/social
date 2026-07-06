package com.mysocial.template

import java.time.Instant
import java.time.LocalTime

data class CreateTemplateRequest(
	val name: String,
	val postId: Long,
	val dispatchTime: LocalTime,
	val keywords: List<String> = emptyList(),
	val dmKeyword: String? = null,
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
	val dispatchTime: LocalTime,
	val keywords: List<String>,
	val dmKeyword: String?,
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
			createdAt = template.createdAt,
		)
	}
}
