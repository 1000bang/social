package com.mysocial.dispatch

import com.mysocial.common.BaseTimeEntity
import com.mysocial.template.AudienceType
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

	@Enumerated(EnumType.STRING)
	@Column(name = "retry_stage")
	var retryStage: DispatchStage? = null
		protected set

	@Enumerated(EnumType.STRING)
	@Column(name = "retry_audience_type")
	var retryAudienceType: AudienceType? = null
		protected set

	@Column(name = "retry_count", nullable = false, columnDefinition = "integer default 0")
	var retryCount: Int = 0
		protected set

	@Column(name = "next_retry_at")
	var nextRetryAt: Instant? = null
		protected set

	fun markAwaitingFollowCheck() {
		status = DispatchStatus.AWAITING_FOLLOW_CHECK
		clearRetry()
	}

	fun markSent(at: Instant) {
		status = DispatchStatus.SENT
		processedAt = at
		clearRetry()
	}

	fun markNonFollowerSent(at: Instant) {
		status = DispatchStatus.NON_FOLLOWER_SENT
		processedAt = at
		clearRetry()
	}

	fun markFailed(at: Instant) {
		status = DispatchStatus.FAILED
		processedAt = at
		clearRetry()
	}

	// 레이트리밋 등 일시적 오류로 재시도가 필요할 때 호출. audienceType은 팔로우 분기 발송 단계에서만 사용된다.
	// countsAsAttempt=false는 실제로 시도해서 실패한 게 아니라, 한도 초과를 예측하고 선제적으로 미룬 경우(재시도 횟수를 소모하지 않음).
	fun markRetryPending(stage: DispatchStage, at: Instant, audienceType: AudienceType? = null, countsAsAttempt: Boolean = true) {
		status = DispatchStatus.RETRY_PENDING
		retryStage = stage
		retryAudienceType = audienceType
		if (countsAsAttempt) retryCount += 1
		nextRetryAt = at
	}

	private fun clearRetry() {
		retryStage = null
		retryAudienceType = null
		nextRetryAt = null
	}
}
