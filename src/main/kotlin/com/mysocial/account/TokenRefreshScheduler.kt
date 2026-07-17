package com.mysocial.account

import com.mysocial.auth.InstagramOAuthClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

private val MIN_TOKEN_AGE_BEFORE_REFRESH: Duration = Duration.ofHours(24)
private val REFRESH_THRESHOLD: Duration = Duration.ofDays(10)
private const val DEFAULT_EXPIRES_IN_SECONDS = 60L * 24 * 3600
private const val FAILURE_REASON_MAX_LENGTH = 255

@Component
class TokenRefreshScheduler(
	private val accountRepository: AccountRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val instagramOAuthClient: InstagramOAuthClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	// Instagram 장기 토큰은 60일마다 만료된다. 매일 새벽에 만료가 임박한 계정만 골라 갱신을 시도해서,
	// 갱신에 실패해도 만료 전까지 며칠의 재시도 여유를 둔다.
	@Scheduled(cron = "0 30 6 * * *", zone = "Asia/Seoul")
	@Transactional
	fun refreshExpiringTokens() {
		accountRepository.findAll().forEach { account ->
			runCatching { refreshIfNeeded(account) }
				.onFailure { ex -> log.error("토큰 갱신 처리 중 예외 발생: accountId={}", account.id, ex) }
		}
	}

	private fun refreshIfNeeded(account: Account) {
		val latest = accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(account.id, TokenRefreshStatus.SUCCESS)
			?: return
		val now = Instant.now()

		if (Duration.between(latest.issuedAt, now) < MIN_TOKEN_AGE_BEFORE_REFRESH) return
		if (Duration.between(now, latest.expiresAt) > REFRESH_THRESHOLD) return

		try {
			val refreshed = instagramOAuthClient.refreshLongLivedToken(latest.encryptedToken)
			accessTokenRepository.save(
				AccessToken(
					account = account,
					encryptedToken = refreshed.accessToken,
					issuedAt = now,
					expiresAt = now.plusSeconds(refreshed.expiresIn ?: DEFAULT_EXPIRES_IN_SECONDS),
					refreshStatus = TokenRefreshStatus.SUCCESS,
				),
			)
			if (account.status == AccountStatus.NEEDS_REAUTH) {
				account.status = AccountStatus.ACTIVE
				accountRepository.save(account)
			}
			log.info("토큰 갱신 성공: accountId={}", account.id)
		} catch (ex: Exception) {
			accessTokenRepository.save(
				AccessToken(
					account = account,
					encryptedToken = latest.encryptedToken,
					issuedAt = latest.issuedAt,
					expiresAt = latest.expiresAt,
					refreshStatus = TokenRefreshStatus.FAILED,
					failureReason = ex.message?.take(FAILURE_REASON_MAX_LENGTH),
				),
			)
			account.status = AccountStatus.NEEDS_REAUTH
			accountRepository.save(account)
			log.warn("토큰 갱신 실패, 재인증 필요: accountId={}", account.id, ex)
		}
	}
}
