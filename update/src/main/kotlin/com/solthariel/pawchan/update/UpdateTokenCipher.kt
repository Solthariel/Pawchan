package com.solthariel.pawchan.update

import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** The key is supplied by Android Keystore; ciphertext is versioned and bound to the application ID. */
internal class UpdateTokenCipher(
    private val key: SecretKey,
    private val applicationId: String,
) {
    fun encrypt(token: String): ByteArray {
        require(validUpdateToken(token)) { "Invalid credential format" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(applicationId.toByteArray(Charsets.UTF_8))
        check(cipher.iv.size == IV_BYTES) { "Unexpected IV size" }
        return byteArrayOf(FORMAT_VERSION) + cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
    }

    fun decrypt(bytes: ByteArray): String {
        require(bytes.size in MIN_CIPHERTEXT_BYTES..MAX_CIPHERTEXT_BYTES && bytes[0] == FORMAT_VERSION) {
            "Invalid stored access data"
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_BITS, bytes.copyOfRange(1, 1 + IV_BYTES)))
        cipher.updateAAD(applicationId.toByteArray(Charsets.UTF_8))
        return String(cipher.doFinal(bytes.copyOfRange(1 + IV_BYTES, bytes.size)), Charsets.UTF_8).also {
            check(validUpdateToken(it)) { "Invalid stored credential" }
        }
    }
}

internal fun validUpdateToken(value: String): Boolean =
    value.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH &&
        value.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' || it == '_' }

internal const val MAX_CIPHERTEXT_BYTES = 512
private const val MIN_CIPHERTEXT_BYTES = 1 + 12 + 16
private const val IV_BYTES = 12
private const val TAG_BITS = 128
private const val FORMAT_VERSION: Byte = 1

private const val MIN_TOKEN_LENGTH = 20
private const val MAX_TOKEN_LENGTH = 255
