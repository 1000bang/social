package com.mysocial.comment

import com.mysocial.common.BaseTimeEntity
import com.mysocial.post.Post
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
	name = "comment",
	uniqueConstraints = [UniqueConstraint(columnNames = ["platform_comment_id"])],
)
class Comment(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	val post: Post,

	@Column(name = "platform_comment_id", nullable = false)
	val platformCommentId: String,

	@Column(name = "author_username")
	val authorUsername: String? = null,

	@Column(nullable = false, columnDefinition = "TEXT")
	val text: String,

	@Column(name = "published_at")
	val publishedAt: Instant? = null,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
