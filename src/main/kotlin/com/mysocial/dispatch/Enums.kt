package com.mysocial.dispatch

enum class TriggerType {
	COMMENT,
	DM_KEYWORD,
}

enum class DispatchStatus {
	PENDING,
	AWAITING_FOLLOW_CHECK,
	NON_FOLLOWER_SENT,
	RETRY_PENDING,
	SENT,
	FAILED,
}

enum class SendResult {
	SUCCESS,
	FAILED,
}

// 발송 실패 후 재시도할 때, 어느 단계부터 재개해야 하는지 표시
enum class DispatchStage {
	INITIAL_PROMPT,
	AUDIENCE_MESSAGE,
}
