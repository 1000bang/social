package com.mysocial.account

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val RETENTION_DAYS = 400L

@Component
class FollowerSnapshotCleanupScheduler(
	private val followerSnapshotRepository: FollowerSnapshotRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Scheduled(cron = "0 30 4 * * *", zone = "Asia/Seoul")
	@Transactional
	fun purgeOld() {
		val deleted = followerSnapshotRepository.deleteByCapturedAtBefore(Instant.now().minus(RETENTION_DAYS, ChronoUnit.DAYS))
		if (deleted > 0) log.info("오래된 팔로워 스냅샷 정리: count={}", deleted)
	}
}
