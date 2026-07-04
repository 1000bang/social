package com.mysocial.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.meta")
data class MetaAppProperties(
	val appId: String,
	val appSecret: String,
)
