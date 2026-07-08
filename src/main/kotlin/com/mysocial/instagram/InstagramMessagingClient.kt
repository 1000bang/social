package com.mysocial.instagram

import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Component
class InstagramMessagingClient {
	private val restClient = RestClient.create("https://graph.instagram.com/v21.0")

	fun sendPrivateReply(accessToken: String, commentId: String, message: Map<String, Any?>) {
		send(accessToken, mapOf("recipient" to mapOf("comment_id" to commentId), "message" to message))
	}

	fun sendDirectMessage(accessToken: String, recipientId: String, message: Map<String, Any?>) {
		send(accessToken, mapOf("recipient" to mapOf("id" to recipientId), "message" to message))
	}

	fun replyToComment(accessToken: String, commentId: String, text: String) {
		restClient.post()
			.uri { builder -> builder.path("/$commentId/replies").queryParam("access_token", accessToken).build() }
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(LinkedMultiValueMap<String, String>().apply { add("message", text) })
			.retrieve()
			.toBodilessEntity()
	}

	private fun send(accessToken: String, body: Map<String, Any?>) {
		restClient.post()
			.uri { builder -> builder.path("/me/messages").queryParam("access_token", accessToken).build() }
			.contentType(MediaType.APPLICATION_JSON)
			.body(body)
			.retrieve()
			.toBodilessEntity()
	}
}
