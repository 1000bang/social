package com.mysocial.template

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalTime

interface TemplateRepository : JpaRepository<Template, Long> {
	fun findByAccountId(accountId: Long): List<Template>
	fun findByPostId(postId: Long): List<Template>
	fun findByAccountIdAndDmKeywordIsNotNull(accountId: Long): List<Template>
	fun findByDispatchTime(dispatchTime: LocalTime): List<Template>
}
