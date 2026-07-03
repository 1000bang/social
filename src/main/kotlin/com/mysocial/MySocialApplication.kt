package com.mysocial

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class MySocialApplication

fun main(args: Array<String>) {
	runApplication<MySocialApplication>(*args)
}
