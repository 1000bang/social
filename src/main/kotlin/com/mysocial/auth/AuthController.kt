package com.mysocial.auth

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
	private val jwtService: JwtService,
	private val refreshTokenService: RefreshTokenService,
	private val authCookieFactory: AuthCookieFactory,
) {

	@PostMapping("/refresh")
	fun refresh(request: HttpServletRequest): ResponseEntity<Void> {
		val refreshToken = request.cookies?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE_NAME }?.value
			?: return unauthorizedWithClearedCookies()

		return when (val result = refreshTokenService.rotate(refreshToken)) {
			is RotateResult.Success -> {
				val newAccessToken = jwtService.issueToken(result.accountId)
				ResponseEntity.noContent()
					.header(HttpHeaders.SET_COOKIE, authCookieFactory.accessTokenCookie(newAccessToken).toString())
					.header(HttpHeaders.SET_COOKIE, authCookieFactory.refreshTokenCookie(result.rawToken).toString())
					.build()
			}
			RotateResult.Invalid, RotateResult.ReuseDetected -> unauthorizedWithClearedCookies()
		}
	}

	@PostMapping("/logout")
	fun logout(request: HttpServletRequest): ResponseEntity<Void> {
		request.cookies?.firstOrNull { it.name == REFRESH_TOKEN_COOKIE_NAME }?.value?.let {
			refreshTokenService.revoke(it)
		}
		return ResponseEntity.noContent()
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredAccessTokenCookie().toString())
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredRefreshTokenCookie().toString())
			.build()
	}

	private fun unauthorizedWithClearedCookies(): ResponseEntity<Void> =
		ResponseEntity.status(HttpStatus.UNAUTHORIZED)
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredAccessTokenCookie().toString())
			.header(HttpHeaders.SET_COOKIE, authCookieFactory.expiredRefreshTokenCookie().toString())
			.build()
}
