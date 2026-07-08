package com.mysocial.media

import com.mysocial.account.AccountRepository
import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import com.mysocial.config.AppServerProperties
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/media")
class MediaController(
	private val mediaAssetRepository: MediaAssetRepository,
	private val accountRepository: AccountRepository,
	private val appServerProperties: AppServerProperties,
) {

	@PostMapping
	fun upload(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestParam file: MultipartFile,
	): Map<String, Any?> {
		require(!file.isEmpty) { "파일이 비어있습니다" }
		val account = accountRepository.findById(accountId)
			.orElseThrow { IllegalArgumentException("계정을 찾을 수 없습니다: $accountId") }

		val asset = mediaAssetRepository.save(
			MediaAsset(
				account = account,
				contentType = file.contentType ?: "application/octet-stream",
				data = file.bytes,
			),
		)

		return mapOf("id" to asset.id, "url" to "${appServerProperties.publicBaseUrl}/api/media/${asset.id}")
	}

	@GetMapping("/{id}")
	fun get(@PathVariable id: Long): ResponseEntity<ByteArray> {
		val asset = mediaAssetRepository.findById(id).orElse(null) ?: return ResponseEntity.notFound().build()
		return ResponseEntity.ok()
			.contentType(MediaType.parseMediaType(asset.contentType))
			.body(asset.data)
	}
}
