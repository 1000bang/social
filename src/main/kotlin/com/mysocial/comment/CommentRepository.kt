package com.mysocial.comment

import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {
	fun existsByPlatformCommentId(platformCommentId: String): Boolean
	fun findTopByPostIdOrderByPublishedAtDesc(postId: Long): Comment?
}
