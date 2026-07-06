package com.mysocial.auth

import com.mysocial.config.MetaAppProperties
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class InstagramOAuthClient(
	private val metaAppProperties: MetaAppProperties,
) {
	private val restClient = RestClient.create("https://graph.facebook.com/v21.0")

	fun buildAuthorizationUrl(state: String): String =
		"https://www.facebook.com/v21.0/dialog/oauth" +
			"?client_id=${metaAppProperties.appId}" +
			"&redirect_uri=${encode(metaAppProperties.redirectUri)}" +
			"&scope=${encode(metaAppProperties.oauthScopes)}" +
			"&response_type=code" +
			"&state=${encode(state)}"

	fun exchangeCodeForLongLivedToken(code: String): String {
		val shortLived = restClient.get()
			.uri { builder ->
				builder.path("/oauth/access_token")
					.queryParam("client_id", metaAppProperties.appId)
					.queryParam("redirect_uri", metaAppProperties.redirectUri)
					.queryParam("client_secret", metaAppProperties.appSecret)
					.queryParam("code", code)
					.build()
			}
			.retrieve()
			.body(TokenExchangeResponse::class.java)
			?: error("단기 토큰 교환 응답이 비어있습니다")

		val longLived = restClient.get()
			.uri { builder ->
				builder.path("/oauth/access_token")
					.queryParam("grant_type", "fb_exchange_token")
					.queryParam("client_id", metaAppProperties.appId)
					.queryParam("client_secret", metaAppProperties.appSecret)
					.queryParam("fb_exchange_token", shortLived.accessToken)
					.build()
			}
			.retrieve()
			.body(TokenExchangeResponse::class.java)
			?: error("장기 토큰 교환 응답이 비어있습니다")

		return longLived.accessToken
	}

	fun fetchInstagramAccount(userAccessToken: String): InstagramAccountInfo {
		val pages = restClient.get()
			.uri { builder -> builder.path("/me/accounts").queryParam("access_token", userAccessToken).build() }
			.retrieve()
			.body(FacebookPagesResponse::class.java)
			?: error("연결된 Facebook 페이지 목록 조회에 실패했습니다")

		for (page in pages.data) {
			val pageDetail = restClient.get()
				.uri { builder ->
					builder.path("/${page.id}")
						.queryParam("fields", "instagram_business_account")
						.queryParam("access_token", userAccessToken)
						.build()
				}
				.retrieve()
				.body(PageInstagramAccount::class.java)

			val instagramAccountId = pageDetail?.instagramBusinessAccount?.id ?: continue

			return restClient.get()
				.uri { builder ->
					builder.path("/$instagramAccountId")
						.queryParam("fields", "id,username")
						.queryParam("access_token", userAccessToken)
						.build()
				}
				.retrieve()
				.body(InstagramAccountInfo::class.java)
				?: error("Instagram 계정 정보 조회에 실패했습니다")
		}

		error("연결된 Instagram 비즈니스 계정을 찾을 수 없습니다. Facebook 페이지에 Instagram 계정을 먼저 연결해주세요.")
	}

	private fun encode(value: String): String =
		java.net.URLEncoder.encode(value, Charsets.UTF_8)
}
