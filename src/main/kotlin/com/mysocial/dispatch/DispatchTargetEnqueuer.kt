package com.mysocial.dispatch

import com.mysocial.template.Template
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DispatchTargetEnqueuer(
	private val dispatchTargetRepository: DispatchTargetRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun enqueue(template: Template, triggerType: TriggerType, platformTriggerId: String, recipientPlatformUserId: String): DispatchTarget? {
		val alreadyQueued = dispatchTargetRepository.existsByTemplateIdAndTriggerTypeAndPlatformTriggerId(
			template.id,
			triggerType,
			platformTriggerId,
		)
		if (alreadyQueued) {
			log.info(
				"dispatchTarget 등록 생략: 이미 존재함 templateId={}, triggerType={}, platformTriggerId={}",
				template.id,
				triggerType,
				platformTriggerId,
			)
			return null
		}

		val saved = dispatchTargetRepository.save(
			DispatchTarget(
				template = template,
				triggerType = triggerType,
				platformTriggerId = platformTriggerId,
				recipientPlatformUserId = recipientPlatformUserId,
			),
		)
		log.info(
			"dispatchTarget 등록 완료: dispatchTargetId={}, templateId={}, triggerType={}, recipientPlatformUserId={}",
			saved.id,
			template.id,
			triggerType,
			recipientPlatformUserId,
		)
		return saved
	}
}
