package com.mysocial.dispatch

enum class TriggerType {
	COMMENT,
	DM_KEYWORD,
}

enum class DispatchStatus {
	PENDING,
	AWAITING_FOLLOW_CHECK,
	SENT,
	FAILED,
}

enum class SendResult {
	SUCCESS,
	FAILED,
}
