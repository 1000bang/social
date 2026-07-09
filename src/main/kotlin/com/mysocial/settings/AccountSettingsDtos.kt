package com.mysocial.settings

data class AccountSettingsResponse(
	val commentReplyText: String?,
	val nonKeywordCommentReplyText: String?,
	val nonFollowerMessageText: String?,
	val postPickerLimit: Int,
) {
	companion object {
		fun from(settings: AccountSettings): AccountSettingsResponse = AccountSettingsResponse(
			commentReplyText = settings.commentReplyText,
			nonKeywordCommentReplyText = settings.nonKeywordCommentReplyText,
			nonFollowerMessageText = settings.nonFollowerMessageText,
			postPickerLimit = settings.postPickerLimit,
		)

		fun default(): AccountSettingsResponse = AccountSettingsResponse(
			commentReplyText = null,
			nonKeywordCommentReplyText = null,
			nonFollowerMessageText = null,
			postPickerLimit = AccountSettings.DEFAULT_POST_PICKER_LIMIT,
		)
	}
}

data class UpdateAccountSettingsRequest(
	val commentReplyText: String?,
	val nonKeywordCommentReplyText: String?,
	val nonFollowerMessageText: String?,
	val postPickerLimit: Int,
)
