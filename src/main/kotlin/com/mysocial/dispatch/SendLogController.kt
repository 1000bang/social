package com.mysocial.dispatch

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/send-logs")
class SendLogController(
	private val sendLogService: SendLogService,
) {

	@GetMapping
	fun list(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): List<SendLogResponse> =
		sendLogService.findByAccount(accountId)
}
