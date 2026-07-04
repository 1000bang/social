package com.mysocial.common

import jakarta.persistence.EntityListeners
import jakarta.persistence.MappedSuperclass
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
abstract class BaseTimeEntity {

	@CreatedDate
	var createdAt: Instant = Instant.EPOCH
		protected set

	@LastModifiedDate
	var updatedAt: Instant = Instant.EPOCH
		protected set
}
