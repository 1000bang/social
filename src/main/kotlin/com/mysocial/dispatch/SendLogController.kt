package com.mysocial.dispatch

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import com.mysocial.common.PageResponse
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/send-logs")
class SendLogController(
	private val sendLogService: SendLogService,
) {

	@GetMapping
	fun list(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestParam(defaultValue = "0") page: Int,
		@RequestParam(defaultValue = "10") size: Int,
	): PageResponse<SendLogResponse> = sendLogService.findByAccount(accountId, page, size)

	@GetMapping("/summary")
	fun summary(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): SendLogSummaryResponse =
		sendLogService.summary(accountId)

	@GetMapping("/chart")
	fun chart(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestParam granularity: ChartGranularity,
	): List<ChartBucket> = sendLogService.chart(accountId, granularity)

	@GetMapping("/top-templates")
	fun topTemplates(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): List<TemplateRankingResponse> =
		sendLogService.topTemplates(accountId)
}
