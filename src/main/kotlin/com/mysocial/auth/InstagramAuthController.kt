package com.mysocial.auth

import com.mysocial.account.Account
import com.mysocial.account.AccountRepository
import com.mysocial.account.AccountStatus
import com.mysocial.account.AccessToken
import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.common.SocialPlatform
import com.mysocial.config.MetaAppProperties
import com.mysocial.instagram.InstagramGraphClient
import com.mysocial.instagram.WEBHOOK_SUBSCRIBED_FIELDS
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.net.URLEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

private const val LONG_LIVED_TOKEN_VALID_DAYS = 60L

@RestController
@RequestMapping("/api/auth/instagram")
class InstagramAuthController(
	private val instagramOAuthClient: InstagramOAuthClient,
	private val accountRepository: AccountRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val jwtService: JwtService,
	private val metaAppProperties: MetaAppProperties,
	private val instagramGraphClient: InstagramGraphClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@GetMapping("/login-url")
	fun loginUrl(): Map<String, String> {
		val state = UUID.randomUUID().toString()
		return mapOf("url" to instagramOAuthClient.buildAuthorizationUrl(state))
	}

	@GetMapping("/callback")
	@Transactional
	fun callback(@RequestParam code: String): ResponseEntity<Void> {
		val redirectUri = runCatching {
			val tokenResult = instagramOAuthClient.exchangeCodeForLongLivedToken(code)
			val igAccount = instagramOAuthClient.fetchInstagramAccount(tokenResult.longLivedToken, tokenResult.instagramUserId)

			val account = accountRepository.findByPlatformAndPlatformAccountId(SocialPlatform.INSTAGRAM, igAccount.id)
				?.also {
					it.username = igAccount.username ?: igAccount.id
					it.status = AccountStatus.ACTIVE
				}
				?: accountRepository.save(
					Account(
						platform = SocialPlatform.INSTAGRAM,
						platformAccountId = igAccount.id,
						username = igAccount.username ?: igAccount.id,
					),
				)

			accessTokenRepository.save(
				AccessToken(
					account = account,
					encryptedToken = tokenResult.longLivedToken,
					issuedAt = Instant.now(),
					expiresAt = Instant.now().plus(LONG_LIVED_TOKEN_VALID_DAYS, ChronoUnit.DAYS),
					refreshStatus = TokenRefreshStatus.SUCCESS,
				),
			)

			runCatching {
				instagramGraphClient.subscribeApp(tokenResult.longLivedToken, igAccount.id, WEBHOOK_SUBSCRIBED_FIELDS)
			}.onFailure { ex ->
				log.warn("웹훅 구독 갱신 실패: accountId={}, platformAccountId={}", account.id, igAccount.id, ex)
			}

			val jwt = jwtService.issueToken(account.id)
			"${metaAppProperties.webAuthCallbackUrl}?token=${encode(jwt)}"
		}.getOrElse { ex ->
			log.warn("Instagram OAuth 콜백 처리 실패", ex)
			"${metaAppProperties.webAuthCallbackUrl}?error=${encode(ex.message ?: "unknown_error")}"
		}

		return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(redirectUri)).build()
	}

	private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8)
}
