package com.mysocial.dispatch

import com.mysocial.template.AudienceType
import org.springframework.data.jpa.domain.Specification
import java.time.Instant

object SendLogSpecifications {

	fun search(
		accountId: Long,
		templateNamePattern: String?,
		audienceType: AudienceType?,
		from: Instant?,
		to: Instant?,
	): Specification<SendLog> = Specification { root, _, cb ->
		val predicates = mutableListOf(cb.equal(root.get<Any>("template").get<Any>("account").get<Long>("id"), accountId))

		if (templateNamePattern != null) {
			predicates += cb.like(cb.lower(root.get<Any>("template").get("name")), templateNamePattern)
		}
		if (audienceType != null) {
			predicates += cb.equal(root.get<AudienceType>("audienceType"), audienceType)
		}
		if (from != null) {
			predicates += cb.greaterThanOrEqualTo(root.get("createdAt"), from)
		}
		if (to != null) {
			predicates += cb.lessThan(root.get("createdAt"), to)
		}

		cb.and(*predicates.toTypedArray())
	}
}
