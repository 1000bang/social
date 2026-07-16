package com.mysocial.dispatch

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface SendLogRepository : JpaRepository<SendLog, Long>, JpaSpecificationExecutor<SendLog> {
	fun deleteByTemplateId(templateId: Long)
	fun countByTemplateAccountIdAndResultAndCreatedAtAfter(accountId: Long, result: SendResult, createdAt: Instant): Long
	fun countByTemplateAccountIdAndResultAndCreatedAtBetween(
		accountId: Long,
		result: SendResult,
		from: Instant,
		to: Instant,
	): Long
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

	@Query(
		"""
		SELECT new com.mysocial.dispatch.TemplateStatRow(s.template.id, COUNT(s))
		FROM SendLog s
		WHERE s.template.account.id = :accountId
		  AND s.result = :result
		  AND s.createdAt BETWEEN :from AND :to
		GROUP BY s.template.id
		""",
	)
	fun countByTemplateGroupedByTemplateBetween(
		@Param("accountId") accountId: Long,
		@Param("result") result: SendResult,
		@Param("from") from: Instant,
		@Param("to") to: Instant,
	): List<TemplateStatRow>
}
