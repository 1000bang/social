package com.mysocial.dispatch

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import com.mysocial.common.PageResponse
import com.mysocial.template.AudienceType
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

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
		@RequestParam(required = false) templateName: String?,
		@RequestParam(required = false) audienceType: AudienceType?,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
	): PageResponse<SendLogResponse> = sendLogService.findByAccount(accountId, page, size, templateName, audienceType, from, to)

	@GetMapping("/summary")
	fun summary(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): SendLogSummaryResponse =
		sendLogService.summary(accountId)

	@GetMapping("/chart")
	fun chart(
		@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long,
		@RequestParam granularity: ChartGranularity,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate?,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
		@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
		@RequestParam(required = false) year: Int?,
	): List<ChartBucket> = sendLogService.chart(accountId, granularity, date, from, to, year)

	@GetMapping("/insights")
	fun insights(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): List<SendLogInsightResponse> =
		sendLogService.insights(accountId)

	@GetMapping("/top-templates")
	fun topTemplates(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): List<TemplateRankingResponse> =
		sendLogService.topTemplates(accountId)
}
