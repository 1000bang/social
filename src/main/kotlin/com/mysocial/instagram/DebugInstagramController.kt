package com.mysocial.instagram

import com.mysocial.account.AccessToken
import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.AccountRepository
import com.mysocial.account.TokenRefreshScheduler
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import com.mysocial.dispatch.DispatchExecutor
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.client.HttpClientErrorException
import java.time.Instant
import java.time.temporal.ChronoUnit

@RestController
@RequestMapping("/api/debug/instagram")
class DebugInstagramController(
	private val accountRepository: AccountRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val instagramGraphClient: InstagramGraphClient,
	private val tokenRefreshScheduler: TokenRefreshScheduler,
	private val dispatchExecutor: DispatchExecutor,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@PostMapping("/refresh-tokens")
	fun refreshTokens(): Map<String, String> {
		tokenRefreshScheduler.refreshExpiringTokens()
		return mapOf("result" to "실행 완료, 로그를 확인하세요")
	}

	// 크론을 기다리지 않고 특정 발송 대상을 즉시 (재)시도해볼 때 사용
	@PostMapping("/dispatch/{dispatchTargetId}/send-initial-prompt")
	fun sendInitialPrompt(@PathVariable dispatchTargetId: Long): Map<String, String> {
		dispatchExecutor.sendInitialPrompt(dispatchTargetId)
		return mapOf("result" to "실행 완료, 로그를 확인하세요")
	}

	@PostMapping("/dispatch/{dispatchTargetId}/retry")
	fun retryDispatch(@PathVariable dispatchTargetId: Long): Map<String, String> {
		dispatchExecutor.retryDispatch(dispatchTargetId)
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

	// 인스타그램 액세스 토큰을 직접 교체한다. VM에서 curl로 호출 가능.
	// curl -X POST "https://social.1000bang.info/api/debug/instagram/update-token?token=NEW_TOKEN&accountId=2"
	@PostMapping("/update-token")
	fun updateToken(
		@RequestParam token: String,
		@RequestParam accountId: Long,
	): Map<String, Any> {
		val account = accountRepository.findById(accountId).orElseThrow { IllegalArgumentException("accountId=$accountId 계정 없음") }
		val saved = accessTokenRepository.save(
			AccessToken(
				account = account,
				encryptedToken = token,
				issuedAt = Instant.now(),
				expiresAt = Instant.now().plus(60, ChronoUnit.DAYS),
				refreshStatus = TokenRefreshStatus.SUCCESS,
			),
		)
		log.info("액세스 토큰 수동 업데이트: accountId={}, newTokenId={}", accountId, saved.id)

		val igUserId = account.platformAccountId
		val subscribeResult = runCatching {
			instagramGraphClient.subscribeApp(token, igUserId, WEBHOOK_SUBSCRIBED_FIELDS)
		}.onSuccess {
			log.info("웹훅 구독 갱신 완료: accountId={}", accountId)
		}.onFailure {
			log.warn("웹훅 구독 갱신 실패: accountId={}", accountId, it)
		}

		return mapOf(
			"tokenId" to saved.id,
			"expiresAt" to saved.expiresAt,
			"webhookSubscribe" to if (subscribeResult.isSuccess) "성공" else "실패: ${subscribeResult.exceptionOrNull()?.message}",
		)
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
