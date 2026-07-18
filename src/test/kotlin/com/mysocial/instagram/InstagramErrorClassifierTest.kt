package com.mysocial.instagram

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import java.io.IOException
import java.nio.charset.StandardCharsets
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class InstagramErrorClassifierTest {

	private val classifier = InstagramErrorClassifier(ObjectMapper())

	private fun graphError(status: HttpStatus, code: Int): HttpClientErrorException {
		val body = """{"error":{"message":"test","type":"OAuthException","code":$code}}"""
		return HttpClientErrorException.create(status, status.reasonPhrase, HttpHeaders.EMPTY, body.toByteArray(), StandardCharsets.UTF_8)
	}

	@Test
	fun `429 상태코드는 재시도 대상이다`() {
		val ex = HttpClientErrorException.create(HttpStatus.TOO_MANY_REQUESTS, "", HttpHeaders.EMPTY, ByteArray(0), null)
		assertTrue(classifier.isRetryable(ex))
	}

	@Test
	fun `앱 단위 요청 한도 초과(code 4)는 재시도 대상이다`() {
		assertTrue(classifier.isRetryable(graphError(HttpStatus.BAD_REQUEST, 4)))
	}

	@Test
	fun `API 호출 한도 초과(code 613)는 재시도 대상이다`() {
		assertTrue(classifier.isRetryable(graphError(HttpStatus.BAD_REQUEST, 613)))
	}

	@Test
	fun `5xx 서버 오류는 재시도 대상이다`() {
		val ex = HttpServerErrorException.create(HttpStatus.SERVICE_UNAVAILABLE, "", HttpHeaders.EMPTY, ByteArray(0), null)
		assertTrue(classifier.isRetryable(ex))
	}

	@Test
	fun `네트워크 타임아웃은 재시도 대상이다`() {
		assertTrue(classifier.isRetryable(ResourceAccessException("timeout", IOException("timeout"))))
	}

	@Test
	fun `유효하지 않은 토큰(code 190)은 재시도 대상이 아니다`() {
		assertFalse(classifier.isRetryable(graphError(HttpStatus.BAD_REQUEST, 190)))
	}

	@Test
	fun `잘못된 요청(rate limit과 무관한 4xx)은 재시도 대상이 아니다`() {
		assertFalse(classifier.isRetryable(graphError(HttpStatus.BAD_REQUEST, 100)))
	}

	@Test
	fun `분류할 수 없는 예외는 재시도 대상이 아니다`() {
		assertFalse(classifier.isRetryable(IllegalStateException("무관한 예외")))
	}
}
