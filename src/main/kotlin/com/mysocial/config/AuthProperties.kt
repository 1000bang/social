package com.mysocial.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.auth")
data class AuthProperties(
	val jwtSecret: String,
	val jwtExpirationDays: Long,
	val cookieSecure: Boolean,
)
