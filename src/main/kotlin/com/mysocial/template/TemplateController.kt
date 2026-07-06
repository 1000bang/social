package com.mysocial.template

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/templates")
class TemplateController(
	private val templateService: TemplateService,
) {

	@PostMapping
	fun create(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestBody request: CreateTemplateRequest,
	): ResponseEntity<TemplateResponse> {
		val template = templateService.create(accountId, request)
		return ResponseEntity.status(HttpStatus.CREATED).body(TemplateResponse.from(template))
	}

	@GetMapping
	fun list(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): List<TemplateResponse> =
		templateService.findByAccount(accountId)

	@DeleteMapping("/{id}")
	fun delete(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@PathVariable id: Long,
	): ResponseEntity<Void> {
		templateService.delete(accountId, id)
		return ResponseEntity.noContent().build()
	}
}
