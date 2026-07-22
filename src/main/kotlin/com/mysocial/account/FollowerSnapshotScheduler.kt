package com.mysocial.account

import com.mysocial.instagram.InstagramGraphClient
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class FollowerSnapshotScheduler(
	private val accountRepository: AccountRepository,
	private val accessTokenRepository: AccessTokenRepository,
	private val followerSnapshotRepository: FollowerSnapshotRepository,
	private val instagramGraphClient: InstagramGraphClient,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@Scheduled(cron = "0 0 5 * * *", zone = "Asia/Seoul")
	@Transactional
	fun captureFollowerCounts() {
		accountRepository.findAll().forEach { account ->
			runCatching { captureForAccount(account) }
				.onFailure { ex -> log.warn("팔로워 수 조회 실패: accountId={}", account.id, ex) }
		}
	}

	private fun captureForAccount(account: Account) {
		val token = accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(account.id, TokenRefreshStatus.SUCCESS)
			?: return
		val followerCount = instagramGraphClient.getFollowerCount(token.encryptedToken) ?: return
		followerSnapshotRepository.save(
			FollowerSnapshot(account = account, followerCount = followerCount, capturedAt = Instant.now()),
		)
		log.info("팔로워 수 스냅샷 저장: accountId={}, followerCount={}", account.id, followerCount)
	}
}
