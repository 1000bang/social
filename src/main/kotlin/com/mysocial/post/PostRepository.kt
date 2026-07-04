package com.mysocial.post

import org.springframework.data.jpa.repository.JpaRepository

interface PostRepository : JpaRepository<Post, Long> {
	fun findByAccountIdAndPlatformPostId(accountId: Long, platformPostId: String): Post?
}
