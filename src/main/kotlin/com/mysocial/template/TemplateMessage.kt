package com.mysocial.template

import com.mysocial.common.BaseTimeEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "template_message")
class TemplateMessage(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	val template: Template,

	@Enumerated(EnumType.STRING)
	@Column(name = "audience_type", nullable = false)
	val audienceType: AudienceType,

	@Column(name = "order_index", nullable = false)
	val orderIndex: Int,

	@Enumerated(EnumType.STRING)
	@Column(name = "message_type", nullable = false)
	val messageType: MessageType,

	@Column(name = "text_content", columnDefinition = "TEXT")
	val textContent: String? = null,

	@Column(name = "image_url")
	val imageUrl: String? = null,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0

	@OneToMany(mappedBy = "templateMessage", cascade = [CascadeType.ALL], orphanRemoval = true)
	val carouselItems: MutableList<CarouselItem> = mutableListOf()
}
