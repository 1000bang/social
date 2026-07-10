package com.mysocial.dispatch

import com.mysocial.template.AudienceType
import java.time.Instant

enum class ChartGranularity { HOUR, DAY, MONTH }

data class SendLogSummaryResponse(
	val contactedUsersThisMonth: Long,
	val messagesSentThisMonth: Long,
)

data class TemplateStatRow(
	val templateId: Long,
	val count: Long,
)

data class TemplateRankingResponse(
	val templateId: Long,
	val templateName: String,
	val contactedUsers: Long,
	val messagesSent: Long,
)

data class ChartBucket(
	val bucket: String,
	val count: Int,
)

data class SendLogResponse(
	val id: Long,
	val templateId: Long,
	val templateName: String,
	val audienceType: AudienceType?,
	val recipientPlatformUserId: String,
	val recipientUsername: String?,
	val result: SendResult,
	val failureReason: String?,
	val createdAt: Instant,
) {
	companion object {
		fun from(sendLog: SendLog): SendLogResponse = SendLogResponse(
			id = sendLog.id,
			templateId = sendLog.template.id,
			templateName = sendLog.template.name,
			audienceType = sendLog.audienceType,
			recipientPlatformUserId = sendLog.recipientPlatformUserId,
			recipientUsername = sendLog.recipientUsername,
			result = sendLog.result,
			failureReason = sendLog.failureReason,
			createdAt = sendLog.createdAt,
		)
	}
}
