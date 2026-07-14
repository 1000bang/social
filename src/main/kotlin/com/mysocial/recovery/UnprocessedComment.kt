package com.mysocial.recovery

import com.mysocial.common.BaseTimeEntity
import com.mysocial.template.Template
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

// 매일 새벽 스케줄이 발견한 미처리 댓글을 쌓아두는 테이블.
// 체크포인트가 전진해도 여기 저장된 항목은 사용자가 직접 처리(또는 일괄 처리)하기 전까지 유실되지 않는다.
@Entity
@Table(
	name = "unprocessed_comment",
	uniqueConstraints = [UniqueConstraint(columnNames = ["template_id", "platform_comment_id"])],
)
class UnprocessedComment(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	val template: Template,

	@Column(name = "platform_comment_id", nullable = false)
	val platformCommentId: String,

	@Column(name = "author_platform_user_id", nullable = false)
	val authorPlatformUserId: String,

	@Column(name = "author_username")
	val authorUsername: String?,

	@Column(columnDefinition = "TEXT", nullable = false)
	val text: String,

	@Column(name = "published_at", nullable = false)
	val publishedAt: Instant,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
