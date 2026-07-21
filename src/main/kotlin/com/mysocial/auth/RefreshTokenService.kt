package com.mysocial.auth

import com.mysocial.account.AccountRepository
import com.mysocial.config.AuthProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

sealed class RotateResult {
	data class Success(val rawToken: String, val accountId: Long) : RotateResult()
	data object Invalid : RotateResult()
	data object ReuseDetected : RotateResult()
}

@Service
class RefreshTokenService(
	private val refreshTokenRepository: RefreshTokenRepository,
	private val accountRepository: AccountRepository,
	private val authProperties: AuthProperties,
) {
	private val log = LoggerFactory.getLogger(javaClass)
	private val secureRandom = SecureRandom()

	@Transactional
	fun issue(accountId: Long): String {
		val account = accountRepository.findById(accountId)
			.orElseThrow { IllegalArgumentException("계정을 찾을 수 없습니다: $accountId") }
		val rawToken = generateRawToken()
		refreshTokenRepository.save(
			RefreshToken(
				account = account,
				tokenHash = hash(rawToken),
				expiresAt = Instant.now().plus(authProperties.refreshTokenExpirationDays, ChronoUnit.DAYS),
			),
		)
		return rawToken
	}

	// 정상 토큰이면 새 토큰을 발급하고 기존 토큰은 폐기 처리한다(rotation).
	// 이미 폐기된 토큰이 다시 들어오면 탈취로 간주해 해당 계정의 모든 refresh token을 무효화한다.
	// 예외를 던지지 않고 결과 타입으로 반환하는 이유: 재사용 탐지 시의 삭제(deleteByAccountId)가
	// 이 트랜잭션 안에서 실행되는데, RuntimeException을 던지면 @Transactional이 전체를 롤백시켜
	// 방금 실행한 삭제까지 함께 취소돼버린다.
	@Transactional
	fun rotate(rawToken: String): RotateResult {
		val stored = refreshTokenRepository.findByTokenHash(hash(rawToken)) ?: return RotateResult.Invalid

		if (stored.revokedAt != null) {
			log.warn("재사용된 refresh token 탐지, 전체 세션 무효화: accountId={}", stored.account.id)
			refreshTokenRepository.deleteByAccountId(stored.account.id)
			return RotateResult.ReuseDetected
		}
		if (Instant.now().isAfter(stored.expiresAt)) {
			return RotateResult.Invalid
		}

		stored.revoke(Instant.now())
		val newRawToken = issue(stored.account.id)
		return RotateResult.Success(newRawToken, stored.account.id)
	}

	@Transactional
	fun revoke(rawToken: String) {
		refreshTokenRepository.findByTokenHash(hash(rawToken))?.let { it.revoke(Instant.now()) }
	}

	private fun generateRawToken(): String {
		val bytes = ByteArray(32)
		secureRandom.nextBytes(bytes)
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
	}

	private fun hash(rawToken: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(Charsets.UTF_8))
		return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
	}
}
