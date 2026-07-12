package com.mysocial.recovery

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/recovery")
class CommentRecoveryController(
	private val commentRecoveryService: CommentRecoveryService,
) {

	@GetMapping("/cards")
	fun cards(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): List<RecoveryCardResponse> =
		commentRecoveryService.listRecoveryCards(accountId)

	@PostMapping("/posts/{postId}/comments/{commentId}/process")
	fun processComment(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@PathVariable postId: Long,
		@PathVariable commentId: String,
	): ResponseEntity<Void> {
		commentRecoveryService.processComment(accountId, postId, commentId)
		return ResponseEntity.noContent().build()
	}

	@PostMapping("/posts/{postId}/process-all")
	fun processAll(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@PathVariable postId: Long,
	): ResponseEntity<Void> {
		commentRecoveryService.processAllForPost(accountId, postId)
		return ResponseEntity.noContent().build()
	}
}
