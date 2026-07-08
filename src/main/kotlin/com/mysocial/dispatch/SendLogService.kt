package com.mysocial.dispatch

import com.mysocial.common.PageResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.ZoneId

private val ZONE = ZoneId.of("Asia/Seoul")

@Service
class SendLogService(
	private val sendLogRepository: SendLogRepository,
	private val dispatchTargetRepository: DispatchTargetRepository,
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
}
