package com.mysocial.dispatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SendLogRepository : JpaRepository<SendLog, Long> {
	fun deleteByTemplateId(templateId: Long)
	fun findByTemplateAccountId(accountId: Long, pageable: Pageable): Page<SendLog>
	fun countByTemplateAccountIdAndResultAndCreatedAtAfter(accountId: Long, result: SendResult, createdAt: Instant): Long
	fun findByTemplateAccountIdAndResultAndCreatedAtBetween(
		accountId: Long,
		result: SendResult,
		from: Instant,
		to: Instant,
	): List<SendLog>

	@Query(
		"""
		SELECT new com.mysocial.dispatch.TemplateStatRow(s.template.id, COUNT(s))
		FROM SendLog s
		WHERE s.template.account.id = :accountId
		  AND s.result = :result
		GROUP BY s.template.id
		""",
	)
	fun countByTemplateGroupedByTemplate(
		@Param("accountId") accountId: Long,
		@Param("result") result: SendResult,
	): List<TemplateStatRow>
}
