package com.mysocial.instagram

import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException

@RestController
@RequestMapping("/api/debug/instagram")
class DebugInstagramController(
	private val accessTokenRepository: AccessTokenRepository,
	private val instagramGraphClient: InstagramGraphClient,
) {

	@GetMapping("/follow-status")
	fun followStatus(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestParam targetUserId: String,
	): Map<String, Any?> {
		val token = accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(accountId, TokenRefreshStatus.SUCCESS)
			?: return mapOf("error" to "저장된 액세스 토큰이 없습니다")

		return try {
			mapOf("result" to instagramGraphClient.getUserProfile(token.encryptedToken, targetUserId))
		} catch (ex: HttpClientErrorException) {
			mapOf("httpStatus" to ex.statusCode.value(), "body" to ex.responseBodyAsString)
		}
	}
}
