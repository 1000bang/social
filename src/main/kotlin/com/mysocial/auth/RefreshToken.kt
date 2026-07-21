package com.mysocial.auth

import com.mysocial.account.Account
import com.mysocial.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "refresh_token")
class RefreshToken(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	val account: Account,

	@Column(name = "token_hash", nullable = false, unique = true)
	val tokenHash: String,

	@Column(name = "expires_at", nullable = false)
	val expiresAt: Instant,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0

	@Column(name = "revoked_at")
	var revokedAt: Instant? = null
		protected set

	fun revoke(at: Instant) {
		revokedAt = at
	}
}
