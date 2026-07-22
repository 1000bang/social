package com.mysocial.account

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/account")
class AccountController(
	private val accountService: AccountService,
	private val followerGrowthService: FollowerGrowthService,
) {

	@GetMapping("/me")
	fun me(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): AccountMeResponse =
		accountService.me(accountId)

	@GetMapping("/follower-growth")
	fun followerGrowth(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): FollowerStatsResponse =
		followerGrowthService.getStats(accountId)
}
