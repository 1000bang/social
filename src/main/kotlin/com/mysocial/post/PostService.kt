package com.mysocial.post

import com.mysocial.account.AccessTokenRepository
import com.mysocial.account.TokenRefreshStatus
import com.mysocial.instagram.InstagramGraphClient
import com.mysocial.instagram.InstagramMediaItem
import com.mysocial.settings.AccountSettingsService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.format.DateTimeFormatter

@Service
class PostService(
	private val accessTokenRepository: AccessTokenRepository,
	private val postRepository: PostRepository,
	private val instagramGraphClient: InstagramGraphClient,
	private val accountSettingsService: AccountSettingsService,
) {

	@Transactional
	fun listFromInstagram(accountId: Long): List<PostResponse> {
		val token = accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(accountId, TokenRefreshStatus.SUCCESS)
			?: return emptyList()
		val limit = accountSettingsService.getPostPickerLimit(accountId)

		return instagramGraphClient.listMedia(token.encryptedToken, limit).data.map { item ->
			val post = upsertPost(accountId, item)
			PostResponse(
				id = post.id,
				platformPostId = item.id,
				caption = item.caption,
				mediaType = item.mediaType,
				thumbnailUrl = item.thumbnailUrl ?: item.mediaUrl,
				permalink = item.permalink,
				timestamp = item.timestamp,
			)
		}
	}

	private fun upsertPost(accountId: Long, item: InstagramMediaItem): Post {
		val existing = postRepository.findByAccountIdAndPlatformPostId(accountId, item.id)
		if (existing != null) return existing

		val account = accessTokenRepository.findTopByAccountIdAndRefreshStatusOrderByIssuedAtDesc(accountId, TokenRefreshStatus.SUCCESS)!!.account
		return postRepository.save(
			Post(
				account = account,
				platformPostId = item.id,
				permalink = item.permalink,
				caption = item.caption,
				postedAt = parseTimestamp(item.timestamp),
			),
		)
	}

	private fun parseTimestamp(value: String?): Instant? {
		if (value == null) return null
		return runCatching { Instant.from(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssZ").parse(value)) }.getOrNull()
	}
}
