package com.mysocial.settings

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/settings")
class AccountSettingsController(
	private val accountSettingsService: AccountSettingsService,
) {

	@GetMapping
	fun get(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): AccountSettingsResponse =
		accountSettingsService.get(accountId)

	@PutMapping
	fun update(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestBody request: UpdateAccountSettingsRequest,
	): AccountSettingsResponse = accountSettingsService.update(accountId, request)
}
