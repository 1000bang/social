package com.mysocial.instagram

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException

// Meta Graph API가 레이트리밋 상황에서 돌려주는 error.code 값들.
// 4: 앱 단위 요청 한도 초과, 17: 사용자 단위 요청 한도 초과, 32: 페이지 단위 요청 한도 초과, 613: API 호출 한도 초과.
private val RATE_LIMIT_ERROR_CODES = setOf(4, 17, 32, 613)

@Component
class InstagramErrorClassifier(
	private val objectMapper: ObjectMapper,
) {

	// 레이트리밋/네트워크 타임아웃처럼 시간이 지나면 해소될 수 있는 오류만 재시도 대상으로 판단한다.
	// 잘못된 토큰, 잘못된 요청 등 재시도해도 똑같이 실패할 오류는 재시도하지 않고 바로 영구 실패 처리한다.
	fun isRetryable(ex: Throwable): Boolean = when (ex) {
		is ResourceAccessException -> true
		is HttpStatusCodeException -> ex.statusCode.value() == 429 || ex.statusCode.is5xxServerError || errorCode(ex) in RATE_LIMIT_ERROR_CODES
		else -> false
	}

	private fun errorCode(ex: HttpStatusCodeException): Int? =
		runCatching { objectMapper.readTree(ex.responseBodyAsString).path("error").path("code").asInt() }.getOrNull()
}
