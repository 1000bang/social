package com.mysocial.auth

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
	fun findByTokenHash(tokenHash: String): RefreshToken?
	fun deleteByAccountId(accountId: Long)
	fun deleteByExpiresAtBefore(before: Instant): Long
}
