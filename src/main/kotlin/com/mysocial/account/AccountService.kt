package com.mysocial.account

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountService(
	private val accountRepository: AccountRepository,
) {

	@Transactional(readOnly = true)
	fun me(accountId: Long): AccountMeResponse {
		val account = accountRepository.findById(accountId).orElseThrow()
		return AccountMeResponse(username = account.username, profilePictureUrl = account.profilePictureUrl, status = account.status)
	}
}
