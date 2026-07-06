package com.mysocial.template

import com.mysocial.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "template_keyword")
class TemplateKeyword(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	val template: Template,

	@Column(nullable = false)
	val keyword: String,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
