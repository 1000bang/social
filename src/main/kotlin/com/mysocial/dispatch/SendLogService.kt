package com.mysocial.dispatch

import com.mysocial.common.PageResponse
import com.mysocial.template.TemplateRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.ZoneId

private val ZONE = ZoneId.of("Asia/Seoul")
private const val TOP_TEMPLATES_LIMIT = 10

@Service
class SendLogService(
	private val sendLogRepository: SendLogRepository,
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val templateRepository: TemplateRepository,
) {

	@Transactional(readOnly = true)
	fun findByAccount(accountId: Long, page: Int, size: Int): PageResponse<SendLogResponse> {
		val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
		return PageResponse.from(sendLogRepository.findByTemplateAccountId(accountId, pageable), SendLogResponse::from)
	}

	@Transactional(readOnly = true)
	fun summary(accountId: Long): SendLogSummaryResponse {
		val startOfMonth = YearMonth.now(ZONE).atDay(1).atStartOfDay(ZONE).toInstant()
		val contacted = dispatchTargetRepository.countDistinctRecipientsSince(accountId, DispatchStatus.PENDING, startOfMonth)
		val sent = sendLogRepository.countByTemplateAccountIdAndResultAndCreatedAtAfter(accountId, SendResult.SUCCESS, startOfMonth)
		return SendLogSummaryResponse(contactedUsersThisMonth = contacted, messagesSentThisMonth = sent)
	}

	@Transactional(readOnly = true)
	fun chart(accountId: Long, granularity: ChartGranularity): List<ChartBucket> {
		val logs = sendLogRepository.findByTemplateAccountIdAndResult(accountId, SendResult.SUCCESS)

		val counts = when (granularity) {
			ChartGranularity.HOUR -> logs.groupingBy { "%02d시".format(it.createdAt.atZone(ZONE).hour) }.eachCount()
			ChartGranularity.DAY -> logs.groupingBy { it.createdAt.atZone(ZONE).toLocalDate().toString() }.eachCount()
			ChartGranularity.MONTH -> logs.groupingBy {
				val date = it.createdAt.atZone(ZONE).toLocalDate()
				"%04d-%02d".format(date.year, date.monthValue)
			}.eachCount()
		}

		return counts.entries.sortedBy { it.key }.map { ChartBucket(it.key, it.value) }
	}

	@Transactional(readOnly = true)
	fun topTemplates(accountId: Long): List<TemplateRankingResponse> {
		val contactedByTemplate = dispatchTargetRepository
			.countDistinctRecipientsByTemplate(accountId, DispatchStatus.PENDING)
			.associate { it.templateId to it.count }
		val sentByTemplate = sendLogRepository
			.countByTemplateGroupedByTemplate(accountId, SendResult.SUCCESS)
			.associate { it.templateId to it.count }

		val templateIds = contactedByTemplate.keys + sentByTemplate.keys
		if (templateIds.isEmpty()) return emptyList()

		val templateNames = templateRepository.findAllById(templateIds).associate { it.id to it.name }

		return templateIds
			.mapNotNull { id ->
				val name = templateNames[id] ?: return@mapNotNull null
				TemplateRankingResponse(
					templateId = id,
					templateName = name,
					contactedUsers = contactedByTemplate[id] ?: 0,
					messagesSent = sentByTemplate[id] ?: 0,
				)
			}
			.sortedByDescending { it.messagesSent }
			.take(TOP_TEMPLATES_LIMIT)
	}
}
