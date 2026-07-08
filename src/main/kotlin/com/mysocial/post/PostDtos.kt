package com.mysocial.post

data class PostResponse(
	val id: Long,
	val platformPostId: String,
	val caption: String?,
	val mediaType: String?,
	val thumbnailUrl: String?,
	val permalink: String?,
	val timestamp: String?,
)
