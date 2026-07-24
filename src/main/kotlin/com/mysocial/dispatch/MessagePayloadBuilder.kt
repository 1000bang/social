package com.mysocial.dispatch

import com.mysocial.template.MessageType
import com.mysocial.template.TemplateMessage

const val FOLLOW_CHECK_PAYLOAD_PREFIX = "FOLLOW_CHECK:"

// 메시지 본문에 이 토큰을 넣으면 발송 시 실제 수신자 사용자명으로 치환된다.
// 동일한 문구를 반복 발송해도 매번 다른 메시지처럼 보이게 해, Meta의 스팸성 반복 발송 탐지를 예방하기 위한 용도.
// 중괄호 두 개를 쓰는 이유: 템플릿 메시지 본문에 사용자가 임의로 { }를 쓸 수도 있어, 우연히 겹치지 않도록 구분한다.
const val USERNAME_PLACEHOLDER = "{{사용자이름}}"
private const val USERNAME_FALLBACK = "고객"

object MessagePayloadBuilder {

	fun promptWithFollowButton(text: String, buttonTitle: String, dispatchTargetId: Long, username: String?): Map<String, Any?> = mapOf(
		"attachment" to mapOf(
			"type" to "template",
			"payload" to mapOf(
				"template_type" to "button",
				"text" to applyUsernamePlaceholder(text, username),
				"buttons" to listOf(
					mapOf(
						"type" to "postback",
						"title" to buttonTitle,
						"payload" to "$FOLLOW_CHECK_PAYLOAD_PREFIX$dispatchTargetId",
					),
				),
			),
		),
	)

	fun fromTemplateMessage(message: TemplateMessage, username: String?): Map<String, Any?> = when (message.messageType) {
		MessageType.TEXT -> mapOf("text" to applyUsernamePlaceholderOrNull(message.textContent, username))

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
					"text" to (applyUsernamePlaceholderOrNull(message.textContent, username) ?: ""),
					"buttons" to message.buttons.sortedBy { it.orderIndex }.map { button ->
						mapOf("type" to "web_url", "url" to button.url, "title" to button.title)
					},
				),
			),
		)
	}

	fun applyUsernamePlaceholder(text: String, username: String?): String =
		text.replace(USERNAME_PLACEHOLDER, username ?: USERNAME_FALLBACK)

	private fun applyUsernamePlaceholderOrNull(text: String?, username: String?): String? =
		text?.let { applyUsernamePlaceholder(it, username) }
}
