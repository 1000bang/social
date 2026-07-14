package com.mysocial.recovery

import org.springframework.data.jpa.repository.JpaRepository

interface UnprocessedCommentRepository : JpaRepository<UnprocessedComment, Long> {
	fun findByTemplateId(templateId: Long): List<UnprocessedComment>
	fun existsByTemplateIdAndPlatformCommentId(templateId: Long, platformCommentId: String): Boolean
	fun deleteByTemplateIdAndPlatformCommentId(templateId: Long, platformCommentId: String)
	fun deleteByTemplateId(templateId: Long)
}
