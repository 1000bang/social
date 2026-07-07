package com.mysocial.dispatch

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SendLogService(
	private val sendLogRepository: SendLogRepository,
) {

	@Transactional(readOnly = true)
	fun findByAccount(accountId: Long): List<SendLogResponse> =
		sendLogRepository.findByTemplateAccountIdOrderByCreatedAtDesc(accountId).map(SendLogResponse::from)
}
