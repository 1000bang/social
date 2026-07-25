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

	@Column(name = "follow_prompt_text", columnDefinition = "TEXT")
	var followPromptText: String? = null,

	@Column(name = "follow_button_title")
	var followButtonTitle: String? = null,

	@Column(name = "post_picker_limit", nullable = false)
	var postPickerLimit: Int = DEFAULT_POST_PICKER_LIMIT,

	@Column(name = "max_messages_per_audience", nullable = false)
	var maxMessagesPerAudience: Int = DEFAULT_MAX_MESSAGES_PER_AUDIENCE,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0

	companion object {
		const val DEFAULT_FOLLOW_PROMPT_TEXT = "댓글 남겨주셔서 감사합니다! 아래 버튼을 누르면 메시지가 발송돼요 😊"
		const val DEFAULT_FOLLOW_BUTTON_TITLE = "메시지 보내주세요!"

		const val DEFAULT_POST_PICKER_LIMIT = 5
		const val MIN_POST_PICKER_LIMIT = 1
		const val MAX_POST_PICKER_LIMIT = 25

		const val DEFAULT_MAX_MESSAGES_PER_AUDIENCE = 3
		const val MIN_MAX_MESSAGES_PER_AUDIENCE = 1
		const val MAX_MAX_MESSAGES_PER_AUDIENCE = 5
	}
}
