package com.mysocial.platform.instagram

import com.mysocial.platform.Comment
import com.mysocial.platform.DirectMessage
import com.mysocial.platform.SocialPlatformClient
import org.springframework.stereotype.Component

@Component
class InstagramClient : SocialPlatformClient {
	override fun fetchComments(accountId: String): List<Comment> {
		TODO("Meta Graph API 연동 구현 예정")
	}

	override fun fetchDirectMessages(accountId: String): List<DirectMessage> {
		TODO("Meta Graph API 연동 구현 예정")
	}
}
