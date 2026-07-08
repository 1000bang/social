package com.mysocial.post

import com.mysocial.auth.CURRENT_ACCOUNT_ID_ATTRIBUTE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/posts")
class PostController(
	private val postService: PostService,
) {

	@GetMapping
	fun list(@RequestAttribute(CURRENT_ACCOUNT_ID_ATTRIBUTE) accountId: Long): List<PostResponse> =
		postService.listFromInstagram(accountId)
}
