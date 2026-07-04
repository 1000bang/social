package com.mysocial.webhook

import com.mysocial.common.SocialPlatform
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.security.MessageDigest
import java.time.Instant

@RestController
@RequestMapping("/webhooks/instagram")
class WebhookController(
	@Value("\${app.webhook.verify-token}")
	private val verifyToken: String,
	private val signatureVerifier: WebhookSignatureVerifier,
	private val webhookEventRepository: WebhookEventRepository,
	private val webhookEventProcessor: WebhookEventProcessor,
) {

	@GetMapping
	fun verify(
		@RequestParam("hub.mode") mode: String,
		@RequestParam("hub.verify_token") token: String,
		@RequestParam("hub.challenge") challenge: String,
	): ResponseEntity<String> {
		return if (mode == "subscribe" && token == verifyToken) {
			ResponseEntity.ok(challenge)
		} else {
			ResponseEntity.status(HttpStatus.FORBIDDEN).build()
		}
	}

	@PostMapping
	fun receive(
		@RequestHeader("X-Hub-Signature-256") signature: String?,
		@org.springframework.web.bind.annotation.RequestBody rawBody: String,
	): ResponseEntity<Void> {
		if (!signatureVerifier.isValid(rawBody, signature)) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
		}

		val eventId = sha256Hex(rawBody)
		if (!webhookEventRepository.existsByEventId(eventId)) {
			val saved = webhookEventRepository.save(
				WebhookEvent(
					platform = SocialPlatform.INSTAGRAM,
					eventId = eventId,
					payload = rawBody,
					receivedAt = Instant.now(),
				),
			)
			webhookEventProcessor.process(saved.id)
		}

		return ResponseEntity.ok().build()
	}

	private fun sha256Hex(input: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
		return digest.joinToString("") { "%02x".format(it) }
	}
}
