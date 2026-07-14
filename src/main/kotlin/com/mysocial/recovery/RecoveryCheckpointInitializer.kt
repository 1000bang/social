package com.mysocial.recovery

import com.mysocial.account.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

// 서버가 뜰 때 체크포인트가 아직 없는 계정에 한해서만 "지금"으로 초기화한다.
// 이미 체크포인트가 있는 계정은 재시작해도 건드리지 않아, 재시작 시점으로 경계가 앞당겨져
// 그 사이의 다운타임 구간을 스케줄이 놓치는 일이 없도록 한다.
@Component
class RecoveryCheckpointInitializer(
	private val accountRepository: AccountRepository,
	private val recoveryCheckpointRepository: RecoveryCheckpointRepository,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	@EventListener(ApplicationReadyEvent::class)
	@Transactional
	fun initializeMissingCheckpoints() {
		val now = Instant.now()
		accountRepository.findAll().forEach { account ->
			if (recoveryCheckpointRepository.findByAccountId(account.id) == null) {
				recoveryCheckpointRepository.save(RecoveryCheckpoint(account = account, lastCheckedAt = now))
				log.info("복구 체크포인트 초기화: accountId={}, lastCheckedAt={}", account.id, now)
			}
		}
	}
}
