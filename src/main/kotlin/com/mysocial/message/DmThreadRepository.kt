package com.mysocial.message

import org.springframework.data.jpa.repository.JpaRepository

interface DmThreadRepository : JpaRepository<DmThread, Long> {
	fun findByAccountIdAndPlatformThreadId(accountId: Long, platformThreadId: String): DmThread?
}
