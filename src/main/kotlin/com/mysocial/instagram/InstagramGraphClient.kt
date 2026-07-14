package com.mysocial.instagram

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriUtils
import java.net.URI
import java.nio.charset.StandardCharsets

const val WEBHOOK_SUBSCRIBED_FIELDS = "comments,messages,messaging_postbacks"
private const val GRAPH_BASE_URL = "https://graph.instagram.com/v21.0"

@Component
class InstagramGraphClient {
	private val restClient = RestClient.create(GRAPH_BASE_URL)

	// 댓글 답글(replies) 중첩 필드는 {}를 포함하는데, UriComponentsBuilder의 queryParam()을 거치면
	// {from}이 URI 템플릿 변수로 오인되거나(퍼센트 인코딩 시) 이중 인코딩되므로, 이 URI만 직접 조립한다.
	private fun encodedFieldsUri(path: String, fields: String, accessToken: String, extraParams: Map<String, String> = emptyMap()): URI {
		val encodedToken = UriUtils.encodeQueryParam(accessToken, StandardCharsets.UTF_8)
		val extra = extraParams.entries.joinToString("") { (key, value) ->
			"&$key=${UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8)}"
		}
		return URI.create("$GRAPH_BASE_URL$path?fields=$fields&access_token=$encodedToken$extra")
	}

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
			.uri(
				encodedFieldsUri(
					"/$mediaId/comments",
					"id,text,timestamp,from,replies%7Bfrom%7Bid,username%7D%7D",
					accessToken,
					if (after != null) mapOf("after" to after) else emptyMap(),
				),
			)
			.retrieve()
			.body(InstagramCommentsPageResponse::class.java)
			?: InstagramCommentsPageResponse()

	fun getComment(accessToken: String, commentId: String): InstagramCommentItem? =
		restClient.get()
			.uri(encodedFieldsUri("/$commentId", "id,text,timestamp,from,replies%7Bfrom%7Bid,username%7D%7D", accessToken))
			.retrieve()
			.body(InstagramCommentItem::class.java)
}
