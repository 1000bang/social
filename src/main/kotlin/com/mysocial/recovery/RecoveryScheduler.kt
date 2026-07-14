package com.mysocial.recovery

import com.mysocial.account.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class RecoveryScheduler(
	private val accountRepository: AccountRepository,
	private val commentRecoveryService: CommentRecoveryService,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	// 체크포인트~지금 사이의 미처리 댓글을 매일 새벽에 미리 확인해 테이블에 쌓아둔다.
	// 미처리 대응 페이지는 이 결과 + 그 이후의 아주 짧은 구간만 실시간으로 확인하면 되므로 훨씬 가벼워진다.
	@Scheduled(cron = "0 0 7 * * *", zone = "Asia/Seoul")
	fun archiveUnprocessedComments() {
		accountRepository.findAll().forEach { account ->
			runCatching {
				commentRecoveryService.archiveUnprocessedComments(account.id)
			}.onFailure { ex ->
				log.warn("미처리 댓글 아카이빙 실패: accountId={}", account.id, ex)
			}
		}
	}
}
