package com.mysocial.dispatch

import com.mysocial.template.AudienceType
import java.time.Instant

data class SendLogResponse(
	val id: Long,
	val templateId: Long,
	val templateName: String,
	val audienceType: AudienceType?,
	val recipientPlatformUserId: String,
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
			result = sendLog.result,
			failureReason = sendLog.failureReason,
			createdAt = sendLog.createdAt,
		)
	}
}
