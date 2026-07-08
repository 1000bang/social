package com.mysocial.template

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import com.mysocial.common.PageResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
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
	fun list(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "10") size: Int,
	): PageResponse<TemplateResponse> = templateService.findByAccount(accountId, page, size)

	@DeleteMapping("/{id}")
	fun delete(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@PathVariable id: Long,
	): ResponseEntity<Void> {
		templateService.delete(accountId, id)
		return ResponseEntity.noContent().build()
	}
}
