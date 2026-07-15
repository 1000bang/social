package com.mysocial.config

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping

@Controller
class SpaController {

	@GetMapping("/login", "/auth/callback", "/home", "/templates", "/send-logs", "/settings", "/recovery")
	fun forwardToIndex(): String = "forward:/index.html"
}
