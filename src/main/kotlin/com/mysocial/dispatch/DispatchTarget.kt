package com.mysocial.dispatch

import com.mysocial.common.BaseTimeEntity
import com.mysocial.template.Template
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
	name = "dispatch_target",
	uniqueConstraints = [UniqueConstraint(columnNames = ["template_id", "trigger_type", "platform_trigger_id"])],
)
class DispatchTarget(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	val template: Template,

	@Enumerated(EnumType.STRING)
	@Column(name = "trigger_type", nullable = false)
	val triggerType: TriggerType,

	@Column(name = "platform_trigger_id", nullable = false)
	val platformTriggerId: String,

	@Column(name = "recipient_platform_user_id", nullable = false)
	val recipientPlatformUserId: String,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: DispatchStatus = DispatchStatus.PENDING
		protected set

	@Column(name = "processed_at")
	var processedAt: Instant? = null
		protected set

	fun markAwaitingFollowCheck() {
		status = DispatchStatus.AWAITING_FOLLOW_CHECK
	}

	fun markSent(at: Instant) {
		status = DispatchStatus.SENT
		processedAt = at
	}

	fun markFailed(at: Instant) {
		status = DispatchStatus.FAILED
		processedAt = at
	}
}
