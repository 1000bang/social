package com.mysocial.dispatch

import com.mysocial.template.TemplateRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalTime
import java.time.ZoneId

@Component
class DispatchScheduler(
	private val templateRepository: TemplateRepository,
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val dispatchExecutor: DispatchExecutor,
) {

	@Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
	@Transactional(readOnly = true)
	fun dispatchScheduledTemplates() {
		val now = LocalTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0)
		templateRepository.findByDispatchTime(now).forEach { template ->
			dispatchTargetRepository.findByStatusAndTemplateId(DispatchStatus.PENDING, template.id).forEach { target ->
				dispatchExecutor.sendInitialPrompt(target.id)
			}
		}
	}
}
