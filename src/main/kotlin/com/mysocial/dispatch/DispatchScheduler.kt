package com.mysocial.dispatch

import com.mysocial.template.TemplateRepository
import org.slf4j.LoggerFactory
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
	private val log = LoggerFactory.getLogger(javaClass)

	// 매분 실행되므로 로그 소음을 줄이기 위해 실제로 예약 발송 대상이 있을 때만 로그를 남긴다.
	@Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
	@Transactional(readOnly = true)
	fun dispatchScheduledTemplates() {
		val now = LocalTime.now(ZoneId.of("Asia/Seoul")).withSecond(0).withNano(0)
		val templates = templateRepository.findByDispatchTime(now).filter { it.activeYn }
		if (templates.isEmpty()) return

		log.info("예약 발송 스케줄러 실행: dispatchTime={}, templateIds={}", now, templates.map { it.id })
		templates.forEach { template ->
			val targets = dispatchTargetRepository.findByStatusAndTemplateId(DispatchStatus.PENDING, template.id)
			log.info("예약 발송 대상 처리: templateId={}, pendingTargetCount={}", template.id, targets.size)
			targets.forEach { target ->
				dispatchExecutor.sendInitialPrompt(target.id)
			}
		}
	}
}
