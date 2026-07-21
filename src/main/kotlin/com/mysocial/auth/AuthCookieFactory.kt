package com.mysocial.auth

import com.mysocial.config.AuthProperties
import org.springframework.http.ResponseCookie
import org.springframework.stereotype.Component
import java.time.Duration

const val ACCESS_TOKEN_COOKIE_NAME = "accessToken"
const val REFRESH_TOKEN_COOKIE_NAME = "refreshToken"
const val REFRESH_TOKEN_COOKIE_PATH = "/api/auth"

@Component
class AuthCookieFactory(
	private val authProperties: AuthProperties,
) {

	fun accessTokenCookie(token: String): ResponseCookie =
		ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, token)
			.httpOnly(true)
			.secure(authProperties.cookieSecure)
			.sameSite("Lax")
			.path("/")
			.maxAge(Duration.ofMinutes(authProperties.jwtExpirationMinutes))
			.build()

	fun refreshTokenCookie(token: String): ResponseCookie =
		ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, token)
			.httpOnly(true)
			.secure(authProperties.cookieSecure)
			.sameSite("Lax")
			.path(REFRESH_TOKEN_COOKIE_PATH)
			.maxAge(Duration.ofDays(authProperties.refreshTokenExpirationDays))
			.build()

	fun expiredAccessTokenCookie(): ResponseCookie =
		ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
			.httpOnly(true).secure(authProperties.cookieSecure).sameSite("Lax").path("/").maxAge(0).build()

	fun expiredRefreshTokenCookie(): ResponseCookie =
		ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
			.httpOnly(true).secure(authProperties.cookieSecure).sameSite("Lax").path(REFRESH_TOKEN_COOKIE_PATH).maxAge(0).build()
}
