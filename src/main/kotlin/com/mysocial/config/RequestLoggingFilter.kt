package com.mysocial.config

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class RequestLoggingFilter : OncePerRequestFilter() {
	private val log = LoggerFactory.getLogger(javaClass)

	override fun shouldNotFilter(request: HttpServletRequest): Boolean = !request.requestURI.startsWith("/api/")

	override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
		val start = System.currentTimeMillis()
		log.info("API 요청 수신: {} {}", request.method, request.requestURI)
		try {
			filterChain.doFilter(request, response)
		} finally {
			val accountId = request.getAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE)
			log.info(
				"API 요청 완료: {} {} -> {} ({}ms) accountId={}",
				request.method,
				request.requestURI,
				response.status,
				System.currentTimeMillis() - start,
				accountId ?: "-",
			)
		}
	}
}
