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

	// 모바일에서 /oauth/authorize로 바로 가면 중간에 인스타그램 앱으로 전환되며 콜백이 끊기는 문제가 있었다.
	// 동일 증상 없이 동작하는 타사 서비스의 로그인 URL 구조(accounts/login/?next=.../oauth/authorize/third_party/...&enable_fb_login=0)를
	// 그대로 재현한 것으로, Meta 공식 문서에 명시된 방식은 아니다.
	fun buildAuthorizationUrl(state: String): String {
		val nextPath = "/oauth/authorize/third_party/" +
			"?redirect_uri=${encode(metaAppProperties.redirectUri)}" +
			"&response_type=code" +
			"&scope=${encode(metaAppProperties.oauthScopes)}" +
			"&state=${encode(state)}" +
			"&enable_fb_login=0" +
			"&client_id=${metaAppProperties.appId}"

		return "https://www.instagram.com/accounts/login/" +
			"?force_authentication" +
			"&platform_app_id=${metaAppProperties.appId}" +
			"&next=${encode(nextPath)}"
	}

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
