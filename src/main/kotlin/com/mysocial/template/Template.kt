package com.mysocial.template

import com.mysocial.account.Account
import com.mysocial.common.BaseTimeEntity
import com.mysocial.post.Post
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "template")
class Template(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	val account: Account,

	@Column(nullable = false)
	var name: String,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	var post: Post,

	@Column(name = "dispatch_time")
	var dispatchTime: LocalTime? = null,

	@Column(name = "dm_keyword")
	var dmKeyword: String? = null,

	@Column(name = "comment_reply_text", columnDefinition = "TEXT")
	var commentReplyText: String? = null,

	@Column(name = "non_keyword_comment_reply_text", columnDefinition = "TEXT")
	var nonKeywordCommentReplyText: String? = null,

	@Column(name = "non_keyword_reply_enabled", nullable = false)
	var nonKeywordReplyEnabled: Boolean = true,

	@Column(name = "active_yn", nullable = false)
	var activeYn: Boolean = true,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0

	@OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
	val keywords: MutableList<TemplateKeyword> = mutableListOf()

	@OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
	val messages: MutableList<TemplateMessage> = mutableListOf()

	fun matchesKeyword(text: String): Boolean =
		keywords.isEmpty() || keywords.any { text.contains(it.keyword, ignoreCase = true) }

	fun resolvedCommentReplyText(): String =
		commentReplyText ?: DEFAULT_COMMENT_REPLY_TEXT

	fun resolvedNonKeywordCommentReplyText(): String =
		nonKeywordCommentReplyText ?: DEFAULT_NON_KEYWORD_COMMENT_REPLY_TEXT

	companion object {
		const val DEFAULT_COMMENT_REPLY_TEXT = "메시지 보냈어요! DM이 안보이면 말씀주세요!"
		const val DEFAULT_NON_KEYWORD_COMMENT_REPLY_TEXT = "댓글 남겨주셔서 감사합니다!"
	}
}
