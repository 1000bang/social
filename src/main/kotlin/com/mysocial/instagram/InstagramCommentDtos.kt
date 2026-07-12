package com.mysocial.instagram

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramCommentsPageResponse(
	val data: List<InstagramCommentItem> = emptyList(),
	val paging: InstagramPaging? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramCommentItem(
	val id: String,
	val text: String? = null,
	val timestamp: String? = null,
	val from: InstagramCommentAuthor? = null,
	val replies: InstagramCommentsPageResponse? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramCommentAuthor(
	val id: String? = null,
	val username: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramPaging(
	val cursors: InstagramPagingCursors? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class InstagramPagingCursors(
	val after: String? = null,
	val before: String? = null,
)
