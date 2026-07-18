package com.mysocial.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.dispatch")
data class DispatchProperties(
	// Meta의 시간당 발송 한도보다 낮춰 잡은 안전 마진. 실제 한도는 계정/신뢰도에 따라 달라질 수 있어 여유를 둔다.
	val hourlySendLimit: Int = 180,
	val maxRetryCount: Int = 5,
)
