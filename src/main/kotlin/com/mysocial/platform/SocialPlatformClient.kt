package com.mysocial.platform

interface SocialPlatformClient {
	fun fetchComments(accountId: String): List<Comment>
	fun fetchDirectMessages(accountId: String): List<DirectMessage>
}

data class Comment(
	val platformCommentId: String,
	val postId: String,
	val text: String,
)

data class DirectMessage(
	val platformMessageId: String,
	val threadId: String,
	val text: String,
)
