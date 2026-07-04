package com.mysocial.webhook

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class WebhookSignatureVerifier(
	@Value("\${app.webhook.app-secret}") private val appSecret: String,
) {
	fun isValid(rawBody: String, signatureHeader: String?): Boolean {
		if (signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX)) return false
		val expectedHex = signatureHeader.removePrefix(SIGNATURE_PREFIX)

		val mac = Mac.getInstance(HMAC_ALGORITHM)
		mac.init(SecretKeySpec(appSecret.toByteArray(Charsets.UTF_8), HMAC_ALGORITHM))
		val computed = mac.doFinal(rawBody.toByteArray(Charsets.UTF_8))
		val computedHex = computed.joinToString("") { "%02x".format(it) }

		return MessageDigest.isEqual(
			computedHex.toByteArray(Charsets.UTF_8),
			expectedHex.toByteArray(Charsets.UTF_8),
		)
	}

	companion object {
		private const val SIGNATURE_PREFIX = "sha256="
		private const val HMAC_ALGORITHM = "HmacSHA256"
	}
}
