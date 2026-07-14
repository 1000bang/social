package com.mysocial.recovery

import org.springframework.data.jpa.repository.JpaRepository

interface RecoveryCheckpointRepository : JpaRepository<RecoveryCheckpoint, Long> {
	fun findByAccountId(accountId: Long): RecoveryCheckpoint?
}
