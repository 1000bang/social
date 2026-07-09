package com.mysocial.settings

import com.mysocial.account.Account
import com.mysocial.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "account_settings")
class AccountSettings(
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false, unique = true)
	val account: Account,

	@Column(name = "comment_reply_text", columnDefinition = "TEXT")
	var commentReplyText: String? = null,

	@Column(name = "non_keyword_comment_reply_text", columnDefinition = "TEXT")
	var nonKeywordCommentReplyText: String? = null,

	@Column(name = "non_follower_message_text", columnDefinition = "TEXT")
	var nonFollowerMessageText: String? = null,

	@Column(name = "post_picker_limit", nullable = false)
	var postPickerLimit: Int = DEFAULT_POST_PICKER_LIMIT,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0

	companion object {
		const val DEFAULT_POST_PICKER_LIMIT = 5
		const val MIN_POST_PICKER_LIMIT = 1
		const val MAX_POST_PICKER_LIMIT = 25
	}
}
