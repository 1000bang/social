package com.mysocial.message

import com.mysocial.common.BaseTimeEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Lob
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.Instant

enum class MessageDirection {
	INBOUND,
	OUTBOUND,
}

@Entity
@Table(
	name = "direct_message",
	uniqueConstraints = [UniqueConstraint(columnNames = ["platform_message_id"])],
)
class DirectMessage(
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "thread_id", nullable = false)
	val thread: DmThread,

	@Column(name = "platform_message_id", nullable = false)
	val platformMessageId: String,

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	val direction: MessageDirection,

	@Lob
	@Column(nullable = false)
	val text: String,

	@Column(name = "sent_at")
	val sentAt: Instant? = null,
) : BaseTimeEntity() {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	val id: Long = 0
}
