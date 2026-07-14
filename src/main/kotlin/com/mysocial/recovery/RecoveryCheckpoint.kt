package com.mysocial.recovery

import com.mysocial.account.Account
import com.mysocial.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant

// 계정별로 "여기까지는 이미 미처리 댓글을 확인했다"는 경계 시각을 기록한다.
// 최초 1회만 서버 시작 시각으로 초기화되고, 이후로는 매일 새벽 스케줄이 성공적으로 끝날 때만 전진한다.
@Entity
@Table(name = "recovery_checkpoint")
class RecoveryCheckpoint(
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false, unique = true)
	val account: Account,

	@Column(name = "last_checked_at", nullable = false)
	var lastCheckedAt: Instant,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
