package com.mysocial.account

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val ZONE = ZoneId.of("Asia/Seoul")

// 스냅샷은 매일 쌓이는 게 정상이라, 기준 시점 근처에 데이터가 없으면(스케줄러가 오래 실패했던 경우 등)
// 훨씬 더 오래된 스냅샷을 "이번 주/이번 달" 기준으로 잘못 쓰지 않도록 허용 오차를 둔다.
private const val WEEK_TOLERANCE_DAYS = 2L
private const val MONTH_TOLERANCE_DAYS = 3L

data class FollowerStatsResponse(
	val currentCount: Int?,
	val weekDelta: Int?,
	val monthDelta: Int?,
	val previousWeekDelta: Int?,
	val weekRangeStart: LocalDate?,
	val weekRangeEnd: LocalDate?,
	val previousWeekRangeStart: LocalDate?,
	val previousWeekRangeEnd: LocalDate?,
)

@Service
class FollowerGrowthService(
	private val followerSnapshotRepository: FollowerSnapshotRepository,
) {

	@Transactional(readOnly = true)
	fun getStats(accountId: Long): FollowerStatsResponse {
		val latest = followerSnapshotRepository.findTopByAccountIdOrderByCapturedAtDesc(accountId)
			?: return FollowerStatsResponse(null, null, null, null, null, null, null, null)

		val now = Instant.now()
		val weekAgo = now.minus(7, ChronoUnit.DAYS)
		val weekBaseline = findBaseline(accountId, weekAgo, WEEK_TOLERANCE_DAYS)

		val twoWeeksAgo = now.minus(14, ChronoUnit.DAYS)
		val twoWeeksBaseline = findBaseline(accountId, twoWeeksAgo, WEEK_TOLERANCE_DAYS)

		val startOfMonth = YearMonth.now(ZONE).atDay(1).atStartOfDay(ZONE).toInstant()
		val monthBaseline = findBaseline(accountId, startOfMonth, MONTH_TOLERANCE_DAYS)

		return FollowerStatsResponse(
			currentCount = latest.followerCount,
			weekDelta = weekBaseline?.let { latest.followerCount - it.followerCount },
			monthDelta = monthBaseline?.let { latest.followerCount - it.followerCount },
			previousWeekDelta = if (weekBaseline != null && twoWeeksBaseline != null) {
				weekBaseline.followerCount - twoWeeksBaseline.followerCount
			} else {
				null
			},
			weekRangeStart = if (weekBaseline != null) LocalDate.ofInstant(weekAgo, ZONE) else null,
			weekRangeEnd = if (weekBaseline != null) LocalDate.ofInstant(now, ZONE) else null,
			previousWeekRangeStart = if (twoWeeksBaseline != null) LocalDate.ofInstant(twoWeeksAgo, ZONE) else null,
			previousWeekRangeEnd = if (twoWeeksBaseline != null) LocalDate.ofInstant(weekAgo, ZONE) else null,
		)
	}

	private fun findBaseline(accountId: Long, target: Instant, toleranceDays: Long): FollowerSnapshot? =
		followerSnapshotRepository.findTopByAccountIdAndCapturedAtBetweenOrderByCapturedAtDesc(
			accountId,
			target.minus(toleranceDays, ChronoUnit.DAYS),
			target,
		)
}
