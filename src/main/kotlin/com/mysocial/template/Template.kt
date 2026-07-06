package com.mysocial.template

import com.mysocial.account.Account
import com.mysocial.common.BaseTimeEntity
import com.mysocial.post.Post
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.LocalTime

@Entity
@Table(name = "template")
class Template(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "account_id", nullable = false)
	val account: Account,

	@Column(nullable = false)
	var name: String,

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	val post: Post,

	@Column(name = "dispatch_time", nullable = false)
	var dispatchTime: LocalTime,

	@Column(name = "dm_keyword")
	var dmKeyword: String? = null,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0

	@OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
	val keywords: MutableList<TemplateKeyword> = mutableListOf()

	@OneToMany(mappedBy = "template", cascade = [CascadeType.ALL], orphanRemoval = true)
	val messages: MutableList<TemplateMessage> = mutableListOf()

	fun matchesKeyword(text: String): Boolean =
		keywords.isEmpty() || keywords.any { text.contains(it.keyword, ignoreCase = true) }
}
