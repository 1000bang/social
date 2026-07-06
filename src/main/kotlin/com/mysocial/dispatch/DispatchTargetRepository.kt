package com.mysocial.dispatch

import org.springframework.data.jpa.repository.JpaRepository

interface DispatchTargetRepository : JpaRepository<DispatchTarget, Long> {
	fun existsByTemplateIdAndTriggerTypeAndPlatformTriggerId(
		templateId: Long,
		triggerType: TriggerType,
		platformTriggerId: String,
	): Boolean

	fun findByStatusAndTemplateId(status: DispatchStatus, templateId: Long): List<DispatchTarget>

	fun deleteByTemplateId(templateId: Long)
}
