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
@Table(name = "message_button")
class MessageButton(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_message_id", nullable = false)
	val templateMessage: TemplateMessage,

	@Column(name = "order_index", nullable = false)
	val orderIndex: Int,

	@Column(nullable = false)
	val title: String,

	@Column(nullable = false)
	val url: String,
) : BaseTimeEntity() {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
