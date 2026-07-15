package com.mysocial.account

import com.mysocial.common.BaseTimeEntity
import com.mysocial.common.SocialPlatform
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

enum class AccountStatus {
	ACTIVE,
	NEEDS_REAUTH,
	DISABLED,
}

@Entity
@Table(
	name = "account",
	uniqueConstraints = [UniqueConstraint(columnNames = ["platform", "platform_account_id"])],
)
class Account(
	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	val platform: SocialPlatform,

	@Column(name = "platform_account_id", nullable = false)
	val platformAccountId: String,

	@Column(nullable = false)
	var username: String,

	@Column(name = "profile_picture_url", columnDefinition = "TEXT")
	var profilePictureUrl: String? = null,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	var status: AccountStatus = AccountStatus.ACTIVE,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
