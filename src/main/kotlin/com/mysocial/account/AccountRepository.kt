package com.mysocial.account

import com.mysocial.common.SocialPlatform
import org.springframework.data.jpa.repository.JpaRepository

interface AccountRepository : JpaRepository<Account, Long> {
	fun findByPlatformAndPlatformAccountId(platform: SocialPlatform, platformAccountId: String): Account?
}
