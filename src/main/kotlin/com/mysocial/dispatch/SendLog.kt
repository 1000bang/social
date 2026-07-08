package com.mysocial.dispatch

import com.mysocial.common.BaseTimeEntity
import com.mysocial.template.AudienceType
import com.mysocial.template.Template
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
import jakarta.persistence.Table

@Entity
@Table(name = "send_log")
class SendLog(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "template_id", nullable = false)
	val template: Template,

	@Enumerated(EnumType.STRING)
	@Column(name = "audience_type")
	val audienceType: AudienceType? = null,

	@Column(name = "recipient_platform_user_id", nullable = false)
	val recipientPlatformUserId: String,

	@Column(name = "recipient_username")
	val recipientUsername: String? = null,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	val result: SendResult,

	@Column(name = "failure_reason")
	val failureReason: String? = null,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
