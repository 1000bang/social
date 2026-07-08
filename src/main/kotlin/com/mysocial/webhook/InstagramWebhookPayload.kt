package com.mysocial.webhook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramWebhookPayload(
	val entry: List<WebhookEntry> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookEntry(
	val id: String,
	val changes: List<WebhookChange> = emptyList(),
	val messaging: List<WebhookMessagingEvent> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookChange(
	val field: String,
	val value: CommentChangeValue? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CommentChangeValue(
	val id: String,
	val text: String? = null,
	val from: WebhookUser? = null,
	val media: WebhookMedia? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookUser(
	val id: String,
	val username: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookMedia(
	val id: String,
	@JsonProperty("media_product_type") val mediaProductType: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookMessagingEvent(
	val sender: WebhookUser? = null,
	val message: WebhookMessageContent? = null,
	val postback: WebhookPostback? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookMessageContent(
	val mid: String? = null,
	val text: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WebhookPostback(
	val payload: String? = null,
	val title: String? = null,
)
