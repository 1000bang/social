package com.mysocial

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling

@EnableJpaAuditing
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
class MySocialApplication

fun main(args: Array<String>) {
	runApplication<MySocialApplication>(*args)
}
