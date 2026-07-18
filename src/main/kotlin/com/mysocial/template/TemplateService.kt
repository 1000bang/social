package com.mysocial.template

import com.mysocial.account.AccountRepository
import com.mysocial.common.PageResponse
import com.mysocial.dispatch.DispatchTargetRepository
import com.mysocial.dispatch.SendLogRepository
import com.mysocial.post.PostRepository
import com.mysocial.recovery.UnprocessedCommentRepository
import com.mysocial.settings.AccountSettingsService
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI

@Service
class TemplateService(
	private val accountRepository: AccountRepository,
	private val postRepository: PostRepository,
	private val templateRepository: TemplateRepository,
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val sendLogRepository: SendLogRepository,
	private val unprocessedCommentRepository: UnprocessedCommentRepository,
	private val accountSettingsService: AccountSettingsService,
) {

	@Transactional
	fun create(accountId: Long, request: CreateTemplateRequest): Template {
		val maxMessages = accountSettingsService.getMaxMessagesPerAudience(accountId)
		require(request.followerMessages.size <= maxMessages) {
			"팔로워 메시지는 최대 ${maxMessages}개까지 설정 가능합니다"
		}
		require(request.nonFollowerMessages.size <= maxMessages) {
			"논팔로워 메시지는 최대 ${maxMessages}개까지 설정 가능합니다"
		}
		ensurePostNotTaken(request.postId)
		ensureDmKeywordNotTaken(accountId, request.dmKeyword)

		val account = accountRepository.findById(accountId)
			.orElseThrow { IllegalArgumentException("계정을 찾을 수 없습니다: $accountId") }
		val post = postRepository.findById(request.postId)
			.orElseThrow { IllegalArgumentException("게시물을 찾을 수 없습니다: ${request.postId}") }

		val template = Template(
			account = account,
			name = request.name,
			post = post,
			dispatchTime = request.dispatchTime,
			dmKeyword = request.dmKeyword,
			commentReplyText = request.commentReplyText,
			nonKeywordCommentReplyText = request.nonKeywordCommentReplyText,
			nonKeywordReplyEnabled = request.nonKeywordReplyEnabled,
		)

		request.keywords.forEach { template.keywords.add(TemplateKeyword(template = template, keyword = it)) }
		addMessages(template, AudienceType.FOLLOWER, request.followerMessages)
		addMessages(template, AudienceType.NON_FOLLOWER, request.nonFollowerMessages)

		return templateRepository.save(template)
	}

	@Transactional(readOnly = true)
	fun findByAccount(accountId: Long, page: Int, size: Int): PageResponse<TemplateResponse> {
		val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
		return PageResponse.from(templateRepository.findByAccountId(accountId, pageable), TemplateResponse::from)
	}

	@Transactional(readOnly = true)
	fun findDetail(accountId: Long, id: Long): TemplateDetailResponse {
		val template = templateRepository.findById(id).orElseThrow { TemplateNotFoundException(id) }
		if (template.account.id != accountId) throw TemplateNotFoundException(id)
		return TemplateDetailResponse.from(template)
	}

	@Transactional
	fun update(accountId: Long, id: Long, request: CreateTemplateRequest): Template {
		val maxMessages = accountSettingsService.getMaxMessagesPerAudience(accountId)
		require(request.followerMessages.size <= maxMessages) {
			"팔로워 메시지는 최대 ${maxMessages}개까지 설정 가능합니다"
		}
		require(request.nonFollowerMessages.size <= maxMessages) {
			"논팔로워 메시지는 최대 ${maxMessages}개까지 설정 가능합니다"
		}

		val template = templateRepository.findById(id).orElseThrow { TemplateNotFoundException(id) }
		if (template.account.id != accountId) throw TemplateNotFoundException(id)
		ensurePostNotTaken(request.postId, excludeTemplateId = id)
		ensureDmKeywordNotTaken(accountId, request.dmKeyword, excludeTemplateId = id)

		val post = postRepository.findById(request.postId)
			.orElseThrow { IllegalArgumentException("게시물을 찾을 수 없습니다: ${request.postId}") }

		template.name = request.name
		template.post = post
		template.dispatchTime = request.dispatchTime
		template.dmKeyword = request.dmKeyword
		template.commentReplyText = request.commentReplyText
		template.nonKeywordCommentReplyText = request.nonKeywordCommentReplyText
		template.nonKeywordReplyEnabled = request.nonKeywordReplyEnabled

		template.keywords.clear()
		request.keywords.forEach { template.keywords.add(TemplateKeyword(template = template, keyword = it)) }

		template.messages.clear()
		addMessages(template, AudienceType.FOLLOWER, request.followerMessages)
		addMessages(template, AudienceType.NON_FOLLOWER, request.nonFollowerMessages)

		return templateRepository.save(template)
	}

	@Transactional
	fun updateActiveYn(accountId: Long, id: Long, activeYn: Boolean): TemplateResponse {
		val template = templateRepository.findById(id).orElseThrow { TemplateNotFoundException(id) }
		if (template.account.id != accountId) throw TemplateNotFoundException(id)

		template.activeYn = activeYn
		return TemplateResponse.from(templateRepository.save(template))
	}

	@Transactional
	fun delete(accountId: Long, id: Long) {
		val template = templateRepository.findById(id).orElseThrow { TemplateNotFoundException(id) }
		if (template.account.id != accountId) throw TemplateNotFoundException(id)

		dispatchTargetRepository.deleteByTemplateId(id)
		sendLogRepository.deleteByTemplateId(id)
		unprocessedCommentRepository.deleteByTemplateId(id)
		templateRepository.deleteById(id)
	}

	private fun isValidButtonUrl(url: String): Boolean {
		val uri = runCatching { URI(url) }.getOrNull() ?: return false
		return uri.scheme == "http" || uri.scheme == "https"
	}

	private fun ensurePostNotTaken(postId: Long, excludeTemplateId: Long? = null) {
		val taken = if (excludeTemplateId != null) {
			templateRepository.existsByPostIdAndIdNot(postId, excludeTemplateId)
		} else {
			templateRepository.existsByPostId(postId)
		}
		require(!taken) { "이미 다른 템플릿에 연결된 게시물입니다. 게시물당 하나의 템플릿만 등록할 수 있습니다" }
	}

	private fun ensureDmKeywordNotTaken(accountId: Long, dmKeyword: String?, excludeTemplateId: Long? = null) {
		if (dmKeyword.isNullOrBlank()) return
		val taken = if (excludeTemplateId != null) {
			templateRepository.existsByAccountIdAndDmKeywordIgnoreCaseAndIdNot(accountId, dmKeyword, excludeTemplateId)
		} else {
			templateRepository.existsByAccountIdAndDmKeywordIgnoreCase(accountId, dmKeyword)
		}
		require(!taken) { "이미 다른 템플릿에 등록된 DM 키워드입니다: $dmKeyword" }
	}

	private fun addMessages(template: Template, audienceType: AudienceType, inputs: List<MessageInput>) {
		inputs.forEachIndexed { index, input ->
			when (input.messageType) {
				MessageType.TEXT -> require(!input.textContent.isNullOrBlank()) { "텍스트 메시지는 내용이 필요합니다" }
				MessageType.IMAGE -> require(!input.imageUrl.isNullOrBlank()) { "이미지 메시지는 이미지 URL이 필요합니다" }
				MessageType.BUTTON -> {
					require(!input.textContent.isNullOrBlank()) { "버튼형 메시지는 내용이 필요합니다" }
					require(input.buttons.isNotEmpty()) { "버튼형 메시지는 최소 1개 버튼이 필요합니다" }
					require(input.buttons.size <= 3) { "버튼은 최대 3개까지 설정 가능합니다" }
					input.buttons.forEach { button ->
						require(isValidButtonUrl(button.url)) { "버튼 URL은 http:// 또는 https://로 시작하는 올바른 URL이어야 합니다: ${button.url}" }
					}
				}
			}

			val message = TemplateMessage(
				template = template,
				audienceType = audienceType,
				orderIndex = index + 1,
				messageType = input.messageType,
				textContent = input.textContent,
				imageUrl = input.imageUrl,
			)
			input.buttons.forEachIndexed { buttonIndex, button ->
				message.buttons.add(
					MessageButton(
						templateMessage = message,
						orderIndex = buttonIndex + 1,
						title = button.title,
						url = button.url,
					),
				)
			}
			template.messages.add(message)
		}
	}
}
