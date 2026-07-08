package com.mysocial.dispatch

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface SendLogRepository : JpaRepository<SendLog, Long> {
	fun deleteByTemplateId(templateId: Long)
	fun findByTemplateAccountId(accountId: Long, pageable: Pageable): Page<SendLog>
	fun countByTemplateAccountIdAndResultAndCreatedAtAfter(accountId: Long, result: SendResult, createdAt: Instant): Long
	fun findByTemplateAccountIdAndResult(accountId: Long, result: SendResult): List<SendLog>
}
