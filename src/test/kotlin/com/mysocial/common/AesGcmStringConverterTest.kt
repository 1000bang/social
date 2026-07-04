package com.mysocial.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class AesGcmStringConverterTest {

	private val converter = AesGcmStringConverter("EnV4fl1ammSVUVEoAiHofXTVtOg3GdV29vmqhnlgwI8=")

	@Test
	fun `암호화한 값을 복호화하면 원문과 같다`() {
		val plainText = "IGAAR-long-lived-token-example"

		val encrypted = converter.convertToDatabaseColumn(plainText)
		val decrypted = converter.convertToEntityAttribute(encrypted)

		assertNotEquals(plainText, encrypted)
		assertEquals(plainText, decrypted)
	}

	@Test
	fun `같은 원문도 매번 다른 암호문을 생성한다`() {
		val plainText = "same-token-value"

		val first = converter.convertToDatabaseColumn(plainText)
		val second = converter.convertToDatabaseColumn(plainText)

		assertNotEquals(first, second)
	}
}
