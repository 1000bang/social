package com.mysocial.account

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
@Table(name = "follower_snapshot")
class FollowerSnapshot(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	val account: Account,

	@Column(name = "follower_count", nullable = false)
	val followerCount: Int,

	@Column(name = "captured_at", nullable = false)
	val capturedAt: Instant,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
