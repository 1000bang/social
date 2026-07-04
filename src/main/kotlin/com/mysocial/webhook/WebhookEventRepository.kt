package com.mysocial.webhook

import org.springframework.data.jpa.repository.JpaRepository

interface WebhookEventRepository : JpaRepository<WebhookEvent, Long> {
	fun existsByEventId(eventId: String): Boolean
}
