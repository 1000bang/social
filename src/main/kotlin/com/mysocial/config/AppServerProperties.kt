package com.mysocial.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.server")
data class AppServerProperties(
	val publicBaseUrl: String,
)
