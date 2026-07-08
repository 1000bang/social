package com.mysocial.dispatch

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface DispatchTargetRepository : JpaRepository<DispatchTarget, Long> {
	fun existsByTemplateIdAndTriggerTypeAndPlatformTriggerId(
		templateId: Long,
		triggerType: TriggerType,
		platformTriggerId: String,
	): Boolean

	fun findByStatusAndTemplateId(status: DispatchStatus, templateId: Long): List<DispatchTarget>

	fun deleteByTemplateId(templateId: Long)

	@Query(
		"""
		SELECT COUNT(DISTINCT d.recipientPlatformUserId)
		FROM DispatchTarget d
		WHERE d.template.account.id = :accountId
		  AND d.status <> :excludedStatus
		  AND d.createdAt >= :from
		""",
	)
	fun countDistinctRecipientsSince(
		@Param("accountId") accountId: Long,
		@Param("excludedStatus") excludedStatus: DispatchStatus,
		@Param("from") from: Instant,
	): Long
}
