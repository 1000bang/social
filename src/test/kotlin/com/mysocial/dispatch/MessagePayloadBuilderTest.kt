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

		val payload = MessagePayloadBuilder.fromTemplateMessage(message, username = null)

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

	@Test
	fun `사용자 이름 플레이스홀더는 실제 사용자명으로 치환된다`() {
		val account = Account(platform = SocialPlatform.INSTAGRAM, platformAccountId = "acc-1", username = "tester")
		val post = Post(account = account, platformPostId = "post-1")
		val template = Template(account = account, name = "테스트", post = post)
		val message = TemplateMessage(
			template = template,
			audienceType = AudienceType.FOLLOWER,
			orderIndex = 1,
			messageType = MessageType.TEXT,
			textContent = "${USERNAME_PLACEHOLDER}님, 댓글 감사합니다!",
		)

		val payload = MessagePayloadBuilder.fromTemplateMessage(message, username = "gildong")

		assertEquals("gildong님, 댓글 감사합니다!", payload["text"])
	}

	@Test
	fun `사용자 이름을 알 수 없으면 대체 문구로 치환된다`() {
		val account = Account(platform = SocialPlatform.INSTAGRAM, platformAccountId = "acc-1", username = "tester")
		val post = Post(account = account, platformPostId = "post-1")
		val template = Template(account = account, name = "테스트", post = post)
		val message = TemplateMessage(
			template = template,
			audienceType = AudienceType.FOLLOWER,
			orderIndex = 1,
			messageType = MessageType.TEXT,
			textContent = "${USERNAME_PLACEHOLDER}님, 댓글 감사합니다!",
		)

		val payload = MessagePayloadBuilder.fromTemplateMessage(message, username = null)

		assertEquals("고객님, 댓글 감사합니다!", payload["text"])
	}

	@Test
	fun `팔로우 확인 프롬프트도 사용자 이름 플레이스홀더를 치환한다`() {
		val payload = MessagePayloadBuilder.promptWithFollowButton(
			text = "${USERNAME_PLACEHOLDER}님, 댓글 감사합니다!",
			buttonTitle = "메시지 보내주세요!",
			dispatchTargetId = 1L,
			username = "gildong",
		)

		@Suppress("UNCHECKED_CAST")
		val attachment = payload["attachment"] as Map<String, Any?>
		@Suppress("UNCHECKED_CAST")
		val templatePayload = attachment["payload"] as Map<String, Any?>
		assertEquals("gildong님, 댓글 감사합니다!", templatePayload["text"])
	}
}
