package com.mysocial.instagram

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

const val WEBHOOK_SUBSCRIBED_FIELDS = "comments,messages,messaging_postbacks"

@Component
class InstagramGraphClient {
	private val restClient = RestClient.create("https://graph.instagram.com/v21.0")

	fun getFollowerCount(accessToken: String): Int? =
		restClient.get()
			.uri { builder ->
				builder.path("/me")
					.queryParam("fields", "followers_count")
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(Map::class.java)
			?.get("followers_count")
			?.let { (it as? Number)?.toInt() }

	fun getUserProfile(accessToken: String, targetUserId: String): Map<*, *> =
		restClient.get()
			.uri { builder ->
				builder.path("/$targetUserId")
					.queryParam("fields", "username,is_user_follow_business,is_business_follow_user")
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(Map::class.java)
			?: emptyMap<String, Any?>()

	fun getSubscribedApps(accessToken: String, igUserId: String): Map<*, *> =
		restClient.get()
			.uri { builder ->
				builder.path("/$igUserId/subscribed_apps")
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(Map::class.java)
			?: emptyMap<String, Any?>()

	fun subscribeApp(accessToken: String, igUserId: String, subscribedFields: String): Map<*, *> =
		restClient.post()
			.uri { builder ->
				builder.path("/$igUserId/subscribed_apps")
					.queryParam("subscribed_fields", subscribedFields)
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(Map::class.java)
			?: emptyMap<String, Any?>()

	fun listMedia(accessToken: String, limit: Int): InstagramMediaListResponse =
		restClient.get()
			.uri { builder ->
				builder.path("/me/media")
					.queryParam("fields", "id,caption,media_type,media_url,thumbnail_url,permalink,timestamp")
					.queryParam("limit", limit.toString())
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(InstagramMediaListResponse::class.java)
			?: InstagramMediaListResponse()

	fun getMediaThumbnail(accessToken: String, mediaId: String): String? =
		restClient.get()
			.uri { builder ->
				builder.path("/$mediaId")
					.queryParam("fields", "thumbnail_url,media_url")
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(InstagramMediaItem::class.java)
			?.let { it.thumbnailUrl ?: it.mediaUrl }

	fun listComments(accessToken: String, mediaId: String, after: String? = null): InstagramCommentsPageResponse =
		restClient.get()
			.uri { builder ->
				builder.path("/$mediaId/comments")
					.queryParam("fields", "id,text,timestamp,from")
					.queryParam("access_token", accessToken)
				if (after != null) builder.queryParam("after", after)
				builder.build()
			}
			.retrieve()
			.body(InstagramCommentsPageResponse::class.java)
			?: InstagramCommentsPageResponse()

	fun getComment(accessToken: String, commentId: String): InstagramCommentItem? =
		restClient.get()
			.uri { builder ->
				builder.path("/$commentId")
					.queryParam("fields", "id,text,timestamp,from")
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(InstagramCommentItem::class.java)

	// Instagram Graph API는 comments 목록 조회 시 replies{from} 같은 중첩 필드 확장을 지원하지 않고,
	// 이 전용 엔드포인트로 조회해도 from 자체를 내려주지 않아 답글 작성자를 식별할 수 없다.
	// 그래서 이 응답은 text로 우리 봇 문구와 비교하는 용도로만 쓴다.
	fun listReplies(accessToken: String, commentId: String): InstagramCommentsPageResponse =
		restClient.get()
			.uri { builder ->
				builder.path("/$commentId/replies")
					.queryParam("fields", "id,from,text")
					.queryParam("access_token", accessToken)
					.build()
			}
			.retrieve()
			.body(InstagramCommentsPageResponse::class.java)
			?: InstagramCommentsPageResponse()
}
