package com.mysocial.webhook

import com.mysocial.common.BaseTimeEntity
import com.mysocial.common.SocialPlatform
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Lob
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
	name = "webhook_event",
	uniqueConstraints = [UniqueConstraint(columnNames = ["event_id"])],
)
class WebhookEvent(
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	val platform: SocialPlatform,

	@Column(name = "event_id", nullable = false)
	val eventId: String,

	@Lob
	@Column(nullable = false)
	val payload: String,

	@Column(name = "received_at", nullable = false)
	val receivedAt: Instant,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0

	@Column(nullable = false)
	var processed: Boolean = false
		protected set

	@Column(name = "processed_at")
	var processedAt: Instant? = null
		protected set

	fun markProcessed(at: Instant) {
		processed = true
		processedAt = at
	}
}
