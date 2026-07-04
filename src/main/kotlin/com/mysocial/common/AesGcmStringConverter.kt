package com.mysocial.common

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Component
@Converter
class AesGcmStringConverter(
	@Value("\${app.token.encryption-key}") base64Key: String,
) : AttributeConverter<String, String> {

	private val secretKey = SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES")
	private val secureRandom = SecureRandom()

	override fun convertToDatabaseColumn(attribute: String?): String? {
		if (attribute == null) return null
		val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
		val cipher = Cipher.getInstance(TRANSFORMATION)
		cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
		val cipherText = cipher.doFinal(attribute.toByteArray(Charsets.UTF_8))
		return Base64.getEncoder().encodeToString(iv + cipherText)
	}

	override fun convertToEntityAttribute(dbData: String?): String? {
		if (dbData == null) return null
		val decoded = Base64.getDecoder().decode(dbData)
		val iv = decoded.copyOfRange(0, IV_LENGTH_BYTES)
		val cipherText = decoded.copyOfRange(IV_LENGTH_BYTES, decoded.size)
		val cipher = Cipher.getInstance(TRANSFORMATION)
		cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
		return String(cipher.doFinal(cipherText), Charsets.UTF_8)
	}

	companion object {
		private const val TRANSFORMATION = "AES/GCM/NoPadding"
		private const val IV_LENGTH_BYTES = 12
		private const val TAG_LENGTH_BITS = 128
	}
}
