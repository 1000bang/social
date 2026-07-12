package com.mysocial.recovery

import java.time.Instant

data class RecoveryCardResponse(
	val postId: Long,
	val templateId: Long,
	val templateName: String,
	val thumbnailUrl: String?,
	val comments: List<RecoveryCommentResponse>,
)

data class RecoveryCommentResponse(
	val commentId: String,
	val authorUsername: String?,
	val text: String,
	val timestamp: Instant,
)
