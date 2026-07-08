package com.mysocial.config

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaController {

	@GetMapping("/login", "/auth/callback", "/templates", "/send-logs")
	fun forwardToIndex(): String = "forward:/index.html"
}
