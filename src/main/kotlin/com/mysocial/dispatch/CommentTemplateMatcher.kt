package com.mysocial.dispatch

import com.mysocial.template.TemplateRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CommentTemplateMatcher(
	private val templateRepository: TemplateRepository,
	private val dispatchTargetEnqueuer: DispatchTargetEnqueuer,
	private val dispatchExecutor: DispatchExecutor,
) {
	private val log = LoggerFactory.getLogger(javaClass)

	fun match(postId: Long, commentPlatformId: String, commenterPlatformUserId: String, commentText: String) {
		log.info("댓글 템플릿 매칭 진입: postId={}, commentId={}, commenterId={}", postId, commentPlatformId, commenterPlatformUserId)
		val templates = templateRepository.findByPostId(postId)
		if (templates.isEmpty()) {
			log.info("댓글 템플릿 매칭 종료: 해당 게시물에 연결된 템플릿 없음 postId={}", postId)
			return
		}

		// 게시물당 템플릿은 1개만 허용되지만, 그 제약이 생기기 전에 만들어진 중복 데이터가 남아있을 수 있어 방어적으로 최신 템플릿 하나만 사용한다.
		val template = templates.maxBy { it.createdAt }
		if (templates.size > 1) {
			log.warn(
				"게시물당 템플릿이 여러 개 존재합니다. 가장 최근 템플릿(id={})만 사용합니다: postId={}, templateIds={}",
				template.id,
				postId,
				templates.map { it.id },
			)
		}

		val matched = template.matchesKeyword(commentText)
		log.info("댓글 키워드 매칭 결과: templateId={}, commentId={}, matched={}", template.id, commentPlatformId, matched)

		if (matched) {
			val target = dispatchTargetEnqueuer.enqueue(template, TriggerType.COMMENT, commentPlatformId, commenterPlatformUserId)
			if (target == null) {
				log.info("댓글 매칭 처리 종료: 이미 등록된 dispatchTarget (중복) templateId={}, commentId={}", template.id, commentPlatformId)
			} else if (template.dispatchTime == null) {
				log.info("초기 발송 즉시 실행: dispatchTargetId={}", target.id)
				dispatchExecutor.sendInitialPrompt(target.id)
			} else {
				log.info("초기 발송 예약됨(스케줄러 대기): dispatchTargetId={}, dispatchTime={}", target.id, template.dispatchTime)
			}
		} else if (template.nonKeywordReplyEnabled) {
			dispatchExecutor.replyToNonMatchingComment(template.id, commentPlatformId)
		} else {
			log.info("비키워드 댓글 응답 생략: 응답하지 않음으로 설정됨 templateId={}, commentId={}", template.id, commentPlatformId)
		}
	}
}
