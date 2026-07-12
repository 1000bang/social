package com.mysocial.dispatch

import com.mysocial.template.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class DmKeywordMatcher(
	private val templateRepository: TemplateRepository,
	private val dispatchTargetEnqueuer: DispatchTargetEnqueuer,
	private val dispatchExecutor: DispatchExecutor,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun match(accountId: Long, messagePlatformId: String, senderPlatformUserId: String, messageText: String) {
		log.info("DM 키워드 매칭 진입: accountId={}, messageId={}, senderId={}", accountId, messagePlatformId, senderPlatformUserId)
		val matchedTemplates = templateRepository.findByAccountIdAndDmKeywordIsNotNull(accountId)
			.filter { it.activeYn }
			.filter { template -> template.dmKeyword?.let { messageText.contains(it, ignoreCase = true) } ?: false }
		log.info("DM 키워드 매칭 결과: accountId={}, matchedTemplateIds={}", accountId, matchedTemplates.map { it.id })

		matchedTemplates.forEach { template ->
			val target = dispatchTargetEnqueuer.enqueue(template, TriggerType.DM_KEYWORD, messagePlatformId, senderPlatformUserId)
			if (target != null && template.dispatchTime == null) {
				dispatchExecutor.sendInitialPrompt(target.id)
			}
		}
	}
}
