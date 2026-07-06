package com.mysocial.dispatch

import org.springframework.data.jpa.repository.JpaRepository

interface SendLogRepository : JpaRepository<SendLog, Long> {
	fun deleteByTemplateId(templateId: Long)
}
