package com.mysocial.dispatch

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant

@Component
class DispatchRetryScheduler(
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val dispatchExecutor: DispatchExecutor,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	// 레이트리밋 등 일시적 오류로 재시도 대기 중인 발송 대상을, 예약된 시각이 지나면 매분 다시 시도한다.
	// 여기서는 트랜잭션을 열지 않는다 — dispatchExecutor.retryDispatch()가 각자 자기 트랜잭션을 새로 시작해야
	// 발송 성공/실패 결과를 실제로 커밋할 수 있다 (읽기 전용 트랜잭션에 물리면 내부 쓰기가 전부 실패한다).
	@Scheduled(cron = "30 * * * * *", zone = "Asia/Seoul")
	fun retryPendingDispatches() {
		val due = dispatchTargetRepository.findByStatusAndNextRetryAtBefore(DispatchStatus.RETRY_PENDING, Instant.now())
		if (due.isEmpty()) return

		log.info("발송 재시도 대상 처리: count={}", due.size)
		due.forEach { target ->
			dispatchExecutor.retryDispatch(target.id)
		}
	}
}
