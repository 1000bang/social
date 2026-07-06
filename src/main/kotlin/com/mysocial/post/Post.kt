package com.mysocial.post

import com.mysocial.account.Account
import com.mysocial.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

@Entity
@Table(
	name = "post",
	uniqueConstraints = [UniqueConstraint(columnNames = ["account_id", "platform_post_id"])],
)
class Post(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	val account: Account,

	@Column(name = "platform_post_id", nullable = false)
	val platformPostId: String,

	@Column(name = "permalink")
	val permalink: String? = null,

	@Column(name = "caption", columnDefinition = "TEXT")
	val caption: String? = null,

	@Column(name = "posted_at")
	val postedAt: Instant? = null,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
