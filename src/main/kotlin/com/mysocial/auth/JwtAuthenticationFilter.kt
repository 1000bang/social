package com.mysocial.auth

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

const val CURRENT_ACCOUNT_ID_ATTRIBUTE = "accountId"

@Component
class JwtAuthenticationFilter(
	private val jwtService: JwtService,
	private val objectMapper: ObjectMapper,
) : OncePerRequestFilter() {

	override fun shouldNotFilter(request: HttpServletRequest): Boolean {
		val path = request.requestURI
		if (!path.startsWith("/api/")) return true
		if (path.startsWith("/api/auth/")) return true
		if (path.startsWith("/api/media/") && request.method == "GET") return true
		return false
	}

	override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
		val header = request.getHeader("Authorization")
		val token = header?.takeIf { it.startsWith("Bearer ") }?.removePrefix("Bearer ")
		val accountId = token?.let { jwtService.parseAccountId(it) }

		if (accountId == null) {
			response.status = HttpStatus.UNAUTHORIZED.value()
			response.characterEncoding = Charsets.UTF_8.name()
			response.contentType = MediaType.APPLICATION_JSON_VALUE
			response.writer.write(objectMapper.writeValueAsString(mapOf("message" to "인증 토큰이 유효하지 않습니다")))
			return
		}

		request.setAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE, accountId)
		filterChain.doFilter(request, response)
	}
}
