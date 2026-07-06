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
@Table(name = "carousel_item")
class CarouselItem(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_message_id", nullable = false)
	val templateMessage: TemplateMessage,

	@Column(name = "order_index", nullable = false)
	val orderIndex: Int,

	@Column(name = "image_url", nullable = false)
	val imageUrl: String,

	val title: String? = null,

	val subtitle: String? = null,

	@Column(name = "button_text")
	val buttonText: String? = null,

	@Column(name = "button_url")
	val buttonUrl: String? = null,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
