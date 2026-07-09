package com.mysocial.settings

import org.springframework.data.jpa.repository.JpaRepository

interface AccountSettingsRepository : JpaRepository<AccountSettings, Long> {
	fun findByAccountId(accountId: Long): AccountSettings?
}
