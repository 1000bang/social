package com.mysocial.auth

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class RefreshTokenCleanupScheduler(
	private val refreshTokenRepository: RefreshTokenRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Scheduled(cron = "0 0 4 * * *", zone = "Asia/Seoul")
	@Transactional
	fun purgeExpired() {
		val deleted = refreshTokenRepository.deleteByExpiresAtBefore(Instant.now())
		if (deleted > 0) log.info("만료된 refresh token 정리: count={}", deleted)
	}
}
