package com.mysocial.auth

import com.mysocial.config.AuthProperties
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
	private val authProperties: AuthProperties,
) {

	@PostMapping("/logout")
	fun logout(): ResponseEntity<Void> {
		val expiredCookie = ResponseCookie.from(ACCESS_TOKEN_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(authProperties.cookieSecure)
			.sameSite("Lax")
			.path("/")
			.maxAge(0)
			.build()
		return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, expiredCookie.toString()).build()
	}
}
