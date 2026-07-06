package com.mysocial.template

import org.springframework.data.jpa.repository.JpaRepository

interface TemplateRepository : JpaRepository<Template, Long> {
	fun findByAccountId(accountId: Long): List<Template>
	fun findByPostId(postId: Long): List<Template>
	fun findByAccountIdAndDmKeywordIsNotNull(accountId: Long): List<Template>
}
