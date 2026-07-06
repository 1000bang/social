package com.mysocial.dispatch

import com.mysocial.template.Template
import org.springframework.stereotype.Service

@Service
class DispatchTargetEnqueuer(
	private val dispatchTargetRepository: DispatchTargetRepository,
) {
	fun enqueue(template: Template, triggerType: TriggerType, platformTriggerId: String, recipientPlatformUserId: String) {
		val alreadyQueued = dispatchTargetRepository.existsByTemplateIdAndTriggerTypeAndPlatformTriggerId(
			template.id,
			triggerType,
			platformTriggerId,
		)
		if (alreadyQueued) return

		dispatchTargetRepository.save(
			DispatchTarget(
				template = template,
				triggerType = triggerType,
				platformTriggerId = platformTriggerId,
				recipientPlatformUserId = recipientPlatformUserId,
			),
		)
	}
}
