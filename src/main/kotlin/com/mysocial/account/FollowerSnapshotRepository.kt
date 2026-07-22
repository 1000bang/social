package com.mysocial.account

import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface FollowerSnapshotRepository : JpaRepository<FollowerSnapshot, Long> {
	fun findTopByAccountIdOrderByCapturedAtDesc(accountId: Long): FollowerSnapshot?

	fun findTopByAccountIdAndCapturedAtBetweenOrderByCapturedAtDesc(
		accountId: Long,
		from: Instant,
		to: Instant,
	): FollowerSnapshot?

	fun deleteByCapturedAtBefore(before: Instant): Long
}
