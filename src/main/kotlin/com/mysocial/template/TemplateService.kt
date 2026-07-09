package com.mysocial.template

import com.mysocial.account.AccountRepository
import com.mysocial.common.PageResponse
import com.mysocial.dispatch.DispatchTargetRepository
import com.mysocial.dispatch.SendLogRepository
import com.mysocial.post.PostRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TemplateService(
	private val accountRepository: AccountRepository,
	private val postRepository: PostRepository,
	private val templateRepository: TemplateRepository,
	private val dispatchTargetRepository: DispatchTargetRepository,
	private val sendLogRepository: SendLogRepository,
) {

	@Transactional
	fun create(accountId: Long, request: CreateTemplateRequest): Template {
		require(request.followerMessages.size <= MAX_MESSAGES_PER_AUDIENCE) {
			"팔로워 메시지는 최대 ${MAX_MESSAGES_PER_AUDIENCE}개까지 설정 가능합니다"
		}
		require(request.nonFollowerMessages.size <= MAX_MESSAGES_PER_AUDIENCE) {
			"논팔로워 메시지는 최대 ${MAX_MESSAGES_PER_AUDIENCE}개까지 설정 가능합니다"
		}
		ensurePostNotTaken(request.postId)

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
		require(request.followerMessages.size <= MAX_MESSAGES_PER_AUDIENCE) {
			"팔로워 메시지는 최대 ${MAX_MESSAGES_PER_AUDIENCE}개까지 설정 가능합니다"
		}
		require(request.nonFollowerMessages.size <= MAX_MESSAGES_PER_AUDIENCE) {
			"논팔로워 메시지는 최대 ${MAX_MESSAGES_PER_AUDIENCE}개까지 설정 가능합니다"
		}

		val template = templateRepository.findById(id).orElseThrow { TemplateNotFoundException(id) }
		if (template.account.id != accountId) throw TemplateNotFoundException(id)
		ensurePostNotTaken(request.postId, excludeTemplateId = id)

		val post = postRepository.findById(request.postId)
			.orElseThrow { IllegalArgumentException("게시물을 찾을 수 없습니다: ${request.postId}") }

		template.name = request.name
		template.post = post
		template.dispatchTime = request.dispatchTime
		template.dmKeyword = request.dmKeyword
		template.commentReplyText = request.commentReplyText
		template.nonKeywordCommentReplyText = request.nonKeywordCommentReplyText

		template.keywords.clear()
		request.keywords.forEach { template.keywords.add(TemplateKeyword(template = template, keyword = it)) }

		template.messages.clear()
		addMessages(template, AudienceType.FOLLOWER, request.followerMessages)
		addMessages(template, AudienceType.NON_FOLLOWER, request.nonFollowerMessages)

		return templateRepository.save(template)
	}

	@Transactional
	fun delete(accountId: Long, id: Long) {
		val template = templateRepository.findById(id).orElseThrow { TemplateNotFoundException(id) }
		if (template.account.id != accountId) throw TemplateNotFoundException(id)

		dispatchTargetRepository.deleteByTemplateId(id)
		sendLogRepository.deleteByTemplateId(id)
		templateRepository.deleteById(id)
	}

	private fun ensurePostNotTaken(postId: Long, excludeTemplateId: Long? = null) {
		val taken = if (excludeTemplateId != null) {
			templateRepository.existsByPostIdAndIdNot(postId, excludeTemplateId)
		} else {
			templateRepository.existsByPostId(postId)
		}
		require(!taken) { "이미 다른 템플릿에 연결된 게시물입니다. 게시물당 하나의 템플릿만 등록할 수 있습니다" }
	}

	private fun addMessages(template: Template, audienceType: AudienceType, inputs: List<MessageInput>) {
		inputs.forEachIndexed { index, input ->
			when (input.messageType) {
				MessageType.TEXT -> require(!input.textContent.isNullOrBlank()) { "텍스트 메시지는 내용이 필요합니다" }
				MessageType.IMAGE -> require(!input.imageUrl.isNullOrBlank()) { "이미지 메시지는 이미지 URL이 필요합니다" }
				MessageType.CAROUSEL -> require(input.carouselItems.isNotEmpty()) { "캐러셀 메시지는 최소 1개 아이템이 필요합니다" }
			}

			val message = TemplateMessage(
				template = template,
				audienceType = audienceType,
				orderIndex = index + 1,
				messageType = input.messageType,
				textContent = input.textContent,
				imageUrl = input.imageUrl,
			)
			input.carouselItems.forEachIndexed { itemIndex, item ->
				message.carouselItems.add(
					CarouselItem(
						templateMessage = message,
						orderIndex = itemIndex + 1,
						imageUrl = item.imageUrl,
						title = item.title,
						subtitle = item.subtitle,
						buttonText = item.buttonText,
						buttonUrl = item.buttonUrl,
					),
				)
			}
			template.messages.add(message)
		}
	}

	companion object {
		private const val MAX_MESSAGES_PER_AUDIENCE = 3
	}
}
