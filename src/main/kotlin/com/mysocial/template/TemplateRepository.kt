package com.mysocial.template

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalTime

interface TemplateRepository : JpaRepository<Template, Long> {
	fun findByAccountId(accountId: Long, pageable: Pageable): Page<Template>
	fun findAllByAccountId(accountId: Long): List<Template>
	fun findByPostId(postId: Long): List<Template>
	fun findByAccountIdAndDmKeywordIsNotNull(accountId: Long): List<Template>
	fun findByDispatchTime(dispatchTime: LocalTime): List<Template>
	fun existsByPostId(postId: Long): Boolean
	fun existsByPostIdAndIdNot(postId: Long, id: Long): Boolean
	fun existsByAccountIdAndDmKeywordIgnoreCase(accountId: Long, dmKeyword: String): Boolean
	fun existsByAccountIdAndDmKeywordIgnoreCaseAndIdNot(accountId: Long, dmKeyword: String, id: Long): Boolean
}
