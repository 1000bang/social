package com.mysocial.account

import com.mysocial.common.AesGcmStringConverter
import com.mysocial.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

enum class TokenRefreshStatus {
	SUCCESS,
	FAILED,
}

@Entity
@Table(name = "access_token")
class AccessToken(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	val account: Account,

	@Convert(converter = AesGcmStringConverter::class)
	@Column(name = "encrypted_token", nullable = false, columnDefinition = "TEXT")
	val encryptedToken: String,

	@Column(name = "issued_at", nullable = false)
	val issuedAt: Instant,

	@Column(name = "expires_at", nullable = false)
	val expiresAt: Instant,

	@Enumerated(EnumType.STRING)
	@Column(name = "refresh_status", nullable = false)
	val refreshStatus: TokenRefreshStatus,

	@Column(name = "failure_reason")
	val failureReason: String? = null,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
