package com.mysocial.settings

import com.mysocial.account.AccountRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AccountSettingsService(
	private val accountRepository: AccountRepository,
	private val accountSettingsRepository: AccountSettingsRepository,
) {

	@Transactional(readOnly = true)
	fun get(accountId: Long): AccountSettingsResponse =
		accountSettingsRepository.findByAccountId(accountId)?.let(AccountSettingsResponse::from)
			?: AccountSettingsResponse.default()

	@Transactional
	fun update(accountId: Long, request: UpdateAccountSettingsRequest): AccountSettingsResponse {
		require(request.postPickerLimit in AccountSettings.MIN_POST_PICKER_LIMIT..AccountSettings.MAX_POST_PICKER_LIMIT) {
			"한번에 보여질 게시글 수는 ${AccountSettings.MIN_POST_PICKER_LIMIT}~${AccountSettings.MAX_POST_PICKER_LIMIT} 사이여야 합니다"
		}

		val settings = accountSettingsRepository.findByAccountId(accountId) ?: AccountSettings(
			account = accountRepository.findById(accountId)
				.orElseThrow { IllegalArgumentException("계정을 찾을 수 없습니다: $accountId") },
		)

		settings.commentReplyText = request.commentReplyText?.takeIf { it.isNotBlank() }
		settings.nonKeywordCommentReplyText = request.nonKeywordCommentReplyText?.takeIf { it.isNotBlank() }
		settings.nonFollowerMessageText = request.nonFollowerMessageText?.takeIf { it.isNotBlank() }
		settings.postPickerLimit = request.postPickerLimit

		return AccountSettingsResponse.from(accountSettingsRepository.save(settings))
	}

	@Transactional(readOnly = true)
	fun getPostPickerLimit(accountId: Long): Int =
		accountSettingsRepository.findByAccountId(accountId)?.postPickerLimit ?: AccountSettings.DEFAULT_POST_PICKER_LIMIT
}
