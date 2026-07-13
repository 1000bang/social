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

		if (matchedTemplates.isEmpty()) return

		// DM 키워드 중복 등록은 생성/수정 시 막고 있지만, 그 이전에 만들어진 중복 데이터가 남아있을 수 있어 방어적으로 최신 템플릿 하나만 사용한다.
		val template = matchedTemplates.maxBy { it.createdAt }
		if (matchedTemplates.size > 1) {
			log.warn(
				"DM 키워드가 여러 템플릿에 중복 등록되어 있습니다. 가장 최근 템플릿(id={})만 사용합니다: accountId={}, templateIds={}",
				template.id,
				accountId,
				matchedTemplates.map { it.id },
			)
		}

		val target = dispatchTargetEnqueuer.enqueue(template, TriggerType.DM_KEYWORD, messagePlatformId, senderPlatformUserId)
		if (target != null && template.dispatchTime == null) {
			dispatchExecutor.sendInitialPrompt(target.id)
		}
	}
}
