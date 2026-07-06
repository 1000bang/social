package com.mysocial.auth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenExchangeResponse(
	@JsonProperty("access_token") val accessToken: String,
	@JsonProperty("expires_in") val expiresIn: Long? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FacebookPage(
	val id: String,
	val name: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class FacebookPagesResponse(
	val data: List<FacebookPage> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramBusinessAccountRef(
	val id: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PageInstagramAccount(
	@JsonProperty("instagram_business_account") val instagramBusinessAccount: InstagramBusinessAccountRef? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramAccountInfo(
	val id: String,
	val username: String? = null,
)
