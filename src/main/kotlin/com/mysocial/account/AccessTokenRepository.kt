package com.mysocial.account

import org.springframework.data.jpa.repository.JpaRepository

interface AccessTokenRepository : JpaRepository<AccessToken, Long> {
	fun findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(
		accountId: Long,
		refreshStatus: TokenRefreshStatus,
	): AccessToken?
}
