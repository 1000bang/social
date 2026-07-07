package com.mysocial.instagram

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class InstagramGraphClient {
	private val restClient = RestClient.create("https://graph.instagram.com/v21.0")

	fun getUserProfile(accessToken: String, targetUserId: String): Map<*, *> =
		restClient.get()
			.uri { builder ->
				builder.path("/$targetUserId")
					.queryParam("fields", "is_user_follow_business,is_business_follow_user")
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(Map::class.java)
			?: emptyMap<String, Any?>()
}
