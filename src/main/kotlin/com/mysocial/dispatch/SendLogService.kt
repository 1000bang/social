package com.mysocial.dispatch

import com.mysocial.common.PageResponse
import com.mysocial.template.AudienceType
import com.mysocial.template.TemplateRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val ZONE = ZoneId.of("Asia/Seoul")
private const val TOP_TEMPLATES_LIMIT = 10
private const val MAX_DAY_RANGE = 10
private const val MAX_LOG_SEARCH_DAYS = 31

@Service
class SendLogService(
	private val sendLogRepository: SendLogRepository,
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val templateRepository: TemplateRepository,
) {

	@Transactional(readOnly = true)
	fun findByAccount(
		accountId: Long,
		page: Int,
		size: Int,
		templateName: String?,
		audienceType: AudienceType?,
		from: LocalDate?,
		to: LocalDate?,
	): PageResponse<SendLogResponse> {
		if (from != null && to != null) {
			require(!to.isBefore(from)) { "종료일은 시작일보다 빠를 수 없습니다" }
			require(ChronoUnit.DAYS.between(from, to) <= MAX_LOG_SEARCH_DAYS) { "최대 ${MAX_LOG_SEARCH_DAYS}일까지 조회할 수 있습니다" }
		}
		val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
		val spec = SendLogSpecifications.search(
			accountId,
			templateName?.trim()?.takeIf { it.isNotEmpty() }?.let { "%${it.lowercase()}%" },
			audienceType,
			from?.atStartOfDay(ZONE)?.toInstant(),
			to?.plusDays(1)?.atStartOfDay(ZONE)?.toInstant(),
		)
		return PageResponse.from(sendLogRepository.findAll(spec, pageable), SendLogResponse::from)
	}

	@Transactional(readOnly = true)
	fun summary(accountId: Long): SendLogSummaryResponse {
		val startOfMonth = YearMonth.now(ZONE).atDay(1).atStartOfDay(ZONE).toInstant()
		val contacted = dispatchTargetRepository.countDistinctRecipientsSince(accountId, DispatchStatus.PENDING, startOfMonth)
		val sent = sendLogRepository.countByTemplateAccountIdAndResultAndCreatedAtAfter(accountId, SendResult.SUCCESS, startOfMonth)
		return SendLogSummaryResponse(contactedUsersThisMonth = contacted, messagesSentThisMonth = sent)
	}

	@Transactional(readOnly = true)
	fun chart(
		accountId: Long,
		granularity: ChartGranularity,
		date: LocalDate?,
		from: LocalDate?,
		to: LocalDate?,
		year: Int?,
	): List<ChartBucket> = when (granularity) {
		ChartGranularity.HOUR -> hourChart(accountId, date ?: LocalDate.now(ZONE))
		ChartGranularity.DAY -> dayChart(accountId, from ?: LocalDate.now(ZONE).minusDays(7), to ?: LocalDate.now(ZONE))
		ChartGranularity.MONTH -> monthChart(accountId, year ?: LocalDate.now(ZONE).year)
	}

	private fun hourChart(accountId: Long, date: LocalDate): List<ChartBucket> {
		val logs = logsBetween(accountId, date, date)
		val counts = logs.groupingBy { it.createdAt.atZone(ZONE).hour }.eachCount()
		return (0..23).map { hour -> ChartBucket("%02d시".format(hour), counts[hour] ?: 0) }
	}

	private fun dayChart(accountId: Long, from: LocalDate, to: LocalDate): List<ChartBucket> {
		require(!to.isBefore(from)) { "종료일은 시작일보다 빠를 수 없습니다" }
		require(ChronoUnit.DAYS.between(from, to) < MAX_DAY_RANGE) { "최대 ${MAX_DAY_RANGE}일까지 조회할 수 있습니다" }

		val logs = logsBetween(accountId, from, to)
		val counts = logs.groupingBy { it.createdAt.atZone(ZONE).toLocalDate() }.eachCount()
		return generateSequence(from) { it.plusDays(1) }
			.takeWhile { !it.isAfter(to) }
			.map { ChartBucket("%02d.%02d".format(it.monthValue, it.dayOfMonth), counts[it] ?: 0) }
			.toList()
	}

	private fun monthChart(accountId: Long, year: Int): List<ChartBucket> {
		val logs = logsBetween(accountId, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31))
		val counts = logs.groupingBy { it.createdAt.atZone(ZONE).monthValue }.eachCount()
		return (1..12).map { month -> ChartBucket("${month}월", counts[month] ?: 0) }
	}

	private fun logsBetween(accountId: Long, from: LocalDate, to: LocalDate) =
		sendLogRepository.findByTemplateAccountIdAndResultAndCreatedAtBetween(
			accountId,
			SendResult.SUCCESS,
			from.atStartOfDay(ZONE).toInstant(),
			to.plusDays(1).atStartOfDay(ZONE).toInstant(),
		)

	@Transactional(readOnly = true)
	fun insights(accountId: Long): List<SendLogInsightResponse> {
		val today = LocalDate.now(ZONE)
		val startOfThisMonth = today.withDayOfMonth(1).atStartOfDay(ZONE).toInstant()
		val startOfLastMonth = today.minusMonths(1).withDayOfMonth(1).atStartOfDay(ZONE).toInstant()
		val now = Instant.now()

		return listOfNotNull(
			monthOverMonthInsight(accountId, startOfLastMonth, startOfThisMonth, now),
			successRateInsight(accountId, startOfThisMonth, now),
			topTemplateInsight(accountId, startOfThisMonth, now),
			peakHourInsight(accountId, startOfThisMonth, now),
		).map { SendLogInsightResponse(it) }
	}

	private fun monthOverMonthInsight(accountId: Long, lastMonthStart: Instant, thisMonthStart: Instant, now: Instant): String? {
		val lastMonthCount =
			sendLogRepository.countByTemplateAccountIdAndResultAndCreatedAtBetween(accountId, SendResult.SUCCESS, lastMonthStart, thisMonthStart)
		if (lastMonthCount == 0L) return null
		val thisMonthCount =
			sendLogRepository.countByTemplateAccountIdAndResultAndCreatedAtBetween(accountId, SendResult.SUCCESS, thisMonthStart, now)
		val growth = (thisMonthCount - lastMonthCount) * 100 / lastMonthCount
		return when {
			growth > 0 -> "이번 달 발송 메시지가 지난달보다 ${growth}% 늘었어요 (${lastMonthCount}건 → ${thisMonthCount}건)"
			growth < 0 -> "이번 달 발송 메시지가 지난달보다 ${-growth}% 줄었어요 (${lastMonthCount}건 → ${thisMonthCount}건)"
			else -> "이번 달 발송 메시지가 지난달과 같은 수준이에요 (${thisMonthCount}건)"
		}
	}

	private fun successRateInsight(accountId: Long, from: Instant, to: Instant): String? {
		val success = sendLogRepository.countByTemplateAccountIdAndResultAndCreatedAtBetween(accountId, SendResult.SUCCESS, from, to)
		val failed = sendLogRepository.countByTemplateAccountIdAndResultAndCreatedAtBetween(accountId, SendResult.FAILED, from, to)
		val total = success + failed
		if (total == 0L) return null
		val rate = success * 100 / total
		return "이번 달 발송 성공률은 ${rate}%예요 (${total}건 중 ${success}건 성공)"
	}

	private fun topTemplateInsight(accountId: Long, from: Instant, to: Instant): String? {
		val top = sendLogRepository.countByTemplateGroupedByTemplateBetween(accountId, SendResult.SUCCESS, from, to)
			.maxByOrNull { it.count } ?: return null
		val name = templateRepository.findById(top.templateId).orElse(null)?.name ?: return null
		return "이번 달 가장 많이 발송된 템플릿은 '${name}'이에요 (${top.count}건)"
	}

	private fun peakHourInsight(accountId: Long, from: Instant, to: Instant): String? {
		val logs = sendLogRepository.findByTemplateAccountIdAndResultAndCreatedAtBetween(accountId, SendResult.SUCCESS, from, to)
		if (logs.isEmpty()) return null
		val peak = logs.groupingBy { it.createdAt.atZone(ZONE).hour }.eachCount().maxByOrNull { it.value } ?: return null
		return "이번 달 메시지는 ${peak.key}시에 가장 많이 발송됐어요 (${peak.value}건)"
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
