package com.mysocial.auth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class ShortLivedTokenResponse(
	@JsonProperty("access_token") val accessToken: String,
	@JsonProperty("user_id") val userId: Long,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LongLivedTokenResponse(
	@JsonProperty("access_token") val accessToken: String,
	@JsonProperty("expires_in") val expiresIn: Long? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramMeResponse(
	@JsonProperty("user_id") val userId: String? = null,
	val id: String? = null,
	val username: String? = null,
	@JsonProperty("profile_picture_url") val profilePictureUrl: String? = null,
)

data class InstagramAccountInfo(
	val id: String,
	val username: String?,
	val profilePictureUrl: String?,
)
