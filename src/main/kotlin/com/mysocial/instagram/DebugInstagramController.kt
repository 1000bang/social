package com.mysocial.instagram

import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.AccountRepository
import com.mysocial.account.TokenRefreshScheduler
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/api/debug/instagram")
class DebugInstagramController(
	private val accountRepository: AccountRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val instagramGraphClient: InstagramGraphClient,
	private val tokenRefreshScheduler: TokenRefreshScheduler,
) {

	@PostMapping("/refresh-tokens")
	fun refreshTokens(): Map<String, String> {
		tokenRefreshScheduler.refreshExpiringTokens()
		return mapOf("result" to "실행 완료, 로그를 확인하세요")
	}

	@GetMapping("/follow-status")
	fun followStatus(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestParam targetUserId: String,
	): Map<String, Any?> = withToken(accountId) { token ->
		instagramGraphClient.getUserProfile(token, targetUserId)
	}

	@GetMapping("/subscribed-apps")
	fun subscribedApps(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
	): Map<String, Any?> = withToken(accountId) { token ->
		val igUserId = accountRepository.findById(accountId).orElseThrow().platformAccountId
		instagramGraphClient.getSubscribedApps(token, igUserId)
	}

	@PostMapping("/subscribe")
	fun subscribe(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestParam(defaultValue = WEBHOOK_SUBSCRIBED_FIELDS) fields: String,
	): Map<String, Any?> = withToken(accountId) { token ->
		val igUserId = accountRepository.findById(accountId).orElseThrow().platformAccountId
		instagramGraphClient.subscribeApp(token, igUserId, fields)
	}

	private fun withToken(accountId: Long, block: (String) -> Any?): Map<String, Any?> {
		val token = accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(accountId, TokenRefreshStatus.SUCCESS)
			?: return mapOf("error" to "저장된 액세스 토큰이 없습니다")

		return try {
			mapOf("result" to block(token.encryptedToken))
		} catch (ex: HttpClientErrorException) {
			mapOf("httpStatus" to ex.statusCode.value(), "body" to ex.responseBodyAsString)
		}
	}
}
