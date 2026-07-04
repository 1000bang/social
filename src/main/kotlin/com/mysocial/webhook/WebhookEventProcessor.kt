package com.mysocial.webhook

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class WebhookEventProcessor(
	private val webhookEventRepository: WebhookEventRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Async
	fun process(webhookEventId: Long) {
		val event = webhookEventRepository.findById(webhookEventId).orElse(null) ?: return
		// TODO: 댓글/DM 등 실제 payload 파싱 및 도메인 반영은 Meta 페이로드 스펙 확정 후 구현
		log.info("Processing webhook event {}", event.eventId)
		event.markProcessed(Instant.now())
		webhookEventRepository.save(event)
	}
}
