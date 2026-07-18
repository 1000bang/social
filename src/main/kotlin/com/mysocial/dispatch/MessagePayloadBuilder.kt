package com.mysocial.dispatch

import com.mysocial.template.MessageType
import com.mysocial.template.TemplateMessage

const val FOLLOW_CHECK_PAYLOAD_PREFIX = "FOLLOW_CHECK:"
private const val FOLLOW_BUTTON_TITLE = "팔로우했어요"

object MessagePayloadBuilder {

	fun promptWithFollowButton(text: String, dispatchTargetId: Long): Map<String, Any?> = mapOf(
		"attachment" to mapOf(
			"type" to "template",
			"payload" to mapOf(
				"template_type" to "button",
				"text" to text,
				"buttons" to listOf(
					mapOf(
						"type" to "postback",
						"title" to FOLLOW_BUTTON_TITLE,
						"payload" to "$FOLLOW_CHECK_PAYLOAD_PREFIX$dispatchTargetId",
					),
				),
			),
		),
	)

	fun fromTemplateMessage(message: TemplateMessage): Map<String, Any?> = when (message.messageType) {
		MessageType.TEXT -> mapOf("text" to message.textContent)

		MessageType.IMAGE -> mapOf(
			"attachment" to mapOf(
				"type" to "image",
				"payload" to mapOf("url" to message.imageUrl),
			),
		)

		MessageType.BUTTON -> mapOf(
			"attachment" to mapOf(
				"type" to "template",
				"payload" to mapOf(
					"template_type" to "button",
					"text" to (message.textContent ?: ""),
					"buttons" to message.buttons.sortedBy { it.orderIndex }.map { button ->
						mapOf("type" to "web_url", "url" to button.url, "title" to button.title)
					},
				),
			),
		)
	}
}
