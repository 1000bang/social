package com.mysocial.instagram

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramMediaListResponse(
	val data: List<InstagramMediaItem> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramMediaItem(
	val id: String,
	val caption: String? = null,
	@JsonProperty("media_type") val mediaType: String? = null,
	@JsonProperty("media_url") val mediaUrl: String? = null,
	@JsonProperty("thumbnail_url") val thumbnailUrl: String? = null,
	val permalink: String? = null,
	val timestamp: String? = null,
)
