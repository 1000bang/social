package com.mysocial.common

import com.mysocial.template.TemplateNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {

	@ExceptionHandler(IllegalArgumentException::class)
	fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<Map<String, String?>> =
		ResponseEntity.status(HttpStatus.BAD_REQUEST).body(mapOf("message" to ex.message))

	@ExceptionHandler(TemplateNotFoundException::class)
	fun handleTemplateNotFound(ex: TemplateNotFoundException): ResponseEntity<Map<String, String?>> =
		ResponseEntity.status(HttpStatus.NOT_FOUND).body(mapOf("message" to ex.message))
}
