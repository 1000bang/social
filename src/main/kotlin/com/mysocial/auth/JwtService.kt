package com.mysocial.auth

import com.mysocial.config.AuthProperties
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Date
import javax.crypto.SecretKey

@Component
class JwtService(
	private val authProperties: AuthProperties,
) {
	private val key: SecretKey = Keys.hmacShaKeyFor(authProperties.jwtSecret.toByteArray(Charsets.UTF_8))

	fun issueToken(accountId: Long): String {
		val now = Instant.now()
		return Jwts.builder()
			.subject(accountId.toString())
			.issuedAt(Date.from(now))
			.expiration(Date.from(now.plus(authProperties.jwtExpirationDays, ChronoUnit.DAYS)))
			.signWith(key)
			.compact()
	}

	fun parseAccountId(token: String): Long? = runCatching {
		Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload.subject.toLong()
	}.getOrNull()
}
