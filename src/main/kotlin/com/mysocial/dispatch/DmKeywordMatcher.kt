package com.mysocial.dispatch

import com.mysocial.template.TemplateRepository
import org.springframework.stereotype.Service

@Service
class DmKeywordMatcher(
	private val templateRepository: TemplateRepository,
	private val dispatchTargetEnqueuer: DispatchTargetEnqueuer,
) {
	fun match(accountId: Long, messagePlatformId: String, senderPlatformUserId: String, messageText: String) {
		templateRepository.findByAccountIdAndDmKeywordIsNotNull(accountId)
			.filter { template -> template.dmKeyword?.let { messageText.contains(it, ignoreCase = true) } ?: false }
			.forEach { template ->
				dispatchTargetEnqueuer.enqueue(template, TriggerType.DM_KEYWORD, messagePlatformId, senderPlatformUserId)
			}
	}
}
