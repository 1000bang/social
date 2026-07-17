package com.mysocial.auth

import com.mysocial.config.MetaAppProperties
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

data class InstagramTokenResult(
	val longLivedToken: String,
	val instagramUserId: String,
)

@Component
class InstagramOAuthClient(
	private val metaAppProperties: MetaAppProperties,
) {
	private val authRestClient = RestClient.create("https://api.instagram.com")
	private val graphRestClient = RestClient.create("https://graph.instagram.com")

	fun buildAuthorizationUrl(state: String): String =
		"https://www.instagram.com/oauth/authorize" +
			"?client_id=${metaAppProperties.appId}" +
			"&redirect_uri=${encode(metaAppProperties.redirectUri)}" +
			"&response_type=code" +
			"&scope=${encode(metaAppProperties.oauthScopes)}" +
			"&state=${encode(state)}"

	fun exchangeCodeForLongLivedToken(code: String): InstagramTokenResult {
		val formData = LinkedMultiValueMap<String, String>().apply {
			add("client_id", metaAppProperties.appId)
			add("client_secret", metaAppProperties.appSecret)
			add("grant_type", "authorization_code")
			add("redirect_uri", metaAppProperties.redirectUri)
			add("code", code)
		}

		val shortLived = authRestClient.post()
			.uri("/oauth/access_token")
			.contentType(MediaType.APPLICATION_FORM_URLENCODED)
			.body(formData)
			.retrieve()
			.body(ShortLivedTokenResponse::class.java)
			?: error("단기 토큰 교환 응답이 비어있습니다")

		val longLived = graphRestClient.get()
			.uri { builder ->
				builder.path("/access_token")
					.queryParam("grant_type", "ig_exchange_token")
					.queryParam("client_secret", metaAppProperties.appSecret)
					.queryParam("access_token", shortLived.accessToken)
					.build()
			}
			.retrieve()
			.body(LongLivedTokenResponse::class.java)
			?: error("장기 토큰 교환 응답이 비어있습니다")

		return InstagramTokenResult(
			longLivedToken = longLived.accessToken,
			instagramUserId = shortLived.userId.toString(),
		)
	}

	// 장기 토큰은 발급 후 24시간이 지나야, 그리고 만료 전에만 갱신할 수 있다. 갱신하면 만료일이 다시 60일 뒤로 늘어난다.
	fun refreshLongLivedToken(currentToken: String): LongLivedTokenResponse =
		graphRestClient.get()
			.uri { builder ->
				builder.path("/refresh_access_token")
					.queryParam("grant_type", "ig_refresh_token")
					.queryParam("access_token", currentToken)
					.build()
			}
			.retrieve()
			.body(LongLivedTokenResponse::class.java)
			?: error("토큰 갱신 응답이 비어있습니다")

	fun fetchInstagramAccount(userAccessToken: String, fallbackUserId: String): InstagramAccountInfo {
		val me = graphRestClient.get()
			.uri { builder ->
				builder.path("/me")
					.queryParam("fields", "user_id,username,profile_picture_url")
					.queryParam("access_token", userAccessToken)
					.build()
			}
			.retrieve()
			.body(InstagramMeResponse::class.java)

		return InstagramAccountInfo(
			id = me?.userId ?: me?.id ?: fallbackUserId,
			username = me?.username,
			profilePictureUrl = me?.profilePictureUrl,
		)
	}

	private fun encode(value: String): String =
		java.net.URLEncoder.encode(value, Charsets.UTF_8)
}
