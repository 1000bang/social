package com.mysocial.dispatch

import com.mysocial.account.Account
import com.mysocial.common.SocialPlatform
import com.mysocial.post.Post
import com.mysocial.template.AudienceType
import com.mysocial.template.MessageButton
import com.mysocial.template.MessageType
import com.mysocial.template.Template
import com.mysocial.template.TemplateMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class MessagePayloadBuilderTest {

	@Test
	fun `버튼형 메시지는 Meta 버튼 템플릿 스키마로 변환된다`() {
		val account = Account(platform = SocialPlatform.INSTAGRAM, platformAccountId = "acc-1", username = "tester")
		val post = Post(account = account, platformPostId = "post-1")
		val template = Template(account = account, name = "테스트", post = post)
		val message = TemplateMessage(
			template = template,
			audienceType = AudienceType.FOLLOWER,
			orderIndex = 1,
			messageType = MessageType.BUTTON,
			textContent = "이벤트에 참여해주셔서 감사합니다!",
		)
		message.buttons.add(MessageButton(templateMessage = message, orderIndex = 1, title = "자세히 보기", url = "https://example.com"))
		message.buttons.add(MessageButton(templateMessage = message, orderIndex = 2, title = "공유하기", url = "https://example.com/share"))

		val payload = MessagePayloadBuilder.fromTemplateMessage(message)

		@Suppress("UNCHECKED_CAST")
		val attachment = payload["attachment"] as Map<String, Any?>
		assertEquals("template", attachment["type"])

		@Suppress("UNCHECKED_CAST")
		val templatePayload = attachment["payload"] as Map<String, Any?>
		assertEquals("button", templatePayload["template_type"])
		assertEquals("이벤트에 참여해주셔서 감사합니다!", templatePayload["text"])

		@Suppress("UNCHECKED_CAST")
		val buttons = templatePayload["buttons"] as List<Map<String, Any?>>
		assertEquals(2, buttons.size)
		assertEquals(mapOf("type" to "web_url", "url" to "https://example.com", "title" to "자세히 보기"), buttons[0])
		assertEquals(mapOf("type" to "web_url", "url" to "https://example.com/share", "title" to "공유하기"), buttons[1])
	}
}
