package com.mysocial.message

import org.springframework.data.jpa.repository.JpaRepository

interface DirectMessageRepository : JpaRepository<DirectMessage, Long> {
	fun existsByPlatformMessageId(platformMessageId: String): Boolean
}
