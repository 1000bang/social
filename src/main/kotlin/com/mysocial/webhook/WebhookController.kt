package com.mysocial.webhook

import com.mysocial.common.SocialPlatform
import org.slf4j.LoggerFactory
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
	private val log = LoggerFactory.getLogger(javaClass)

	@GetMapping
	fun verify(
		@RequestParam("hub.mode") mode: String,
		@RequestParam("hub.verify_token") token: String,
		@RequestParam("hub.challenge") challenge: String,
	): ResponseEntity<String> {
		log.info("웹훅 구독 검증 요청 도착: mode={}, tokenMatch={}", mode, token == verifyToken)
		return if (mode == "subscribe" && token == verifyToken) {
			ResponseEntity.ok(challenge)
		} else {
			log.warn("웹훅 구독 검증 실패: mode={}, tokenMatch={}", mode, token == verifyToken)
			ResponseEntity.status(HttpStatus.FORBIDDEN).build()
		}
	}

	@PostMapping
	fun receive(
		@RequestHeader("X-Hub-Signature-256") signature: String?,
		@org.springframework.web.bind.annotation.RequestBody rawBody: String,
	): ResponseEntity<Void> {
		log.info("웹훅 POST 도착: bodyLength={}, signaturePresent={}", rawBody.length, signature != null)

		if (!signatureVerifier.isValid(rawBody, signature)) {
			log.warn(
				"웹훅 서명 검증 실패: signaturePresent={}, bodyLength={}, bodyPreview={}",
				signature != null,
				rawBody.length,
				rawBody.take(300),
			)
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
			log.info("신규 웹훅 이벤트 저장: webhookEventId={}, eventId={}", saved.id, eventId)
			webhookEventProcessor.process(saved.id)
		} else {
			log.info("중복 웹훅 이벤트 무시: eventId={}", eventId)
		}

		return ResponseEntity.ok().build()
	}

	private fun sha256Hex(input: String): String {
		val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
		return digest.joinToString("") { "%02x".format(it) }
	}
}
