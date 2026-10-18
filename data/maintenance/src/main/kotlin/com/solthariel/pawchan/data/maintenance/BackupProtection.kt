package com.solthariel.pawchan.data.maintenance

import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Versioned envelope. The complete ZIP, including its directory, is authenticated and encrypted. */
object BackupProtection {
    private val magic = "PAWBAK".toByteArray(Charsets.US_ASCII) + byteArrayOf(ENVELOPE_VERSION.toByte())
    private const val ENVELOPE_VERSION = 3
    private const val SALT_OFFSET = 12
    const val MIN_PASSWORD_CHARS = 10
    private const val ITERATIONS = 600_000
    private const val SALT_BYTES = 16
    private const val NONCE_OFFSET = SALT_OFFSET + SALT_BYTES
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val KEY_BITS = 256
    private const val HEADER_BYTES = 40
    const val MAX_FILE_BYTES = 32 * 1024 * 1024
    const val MAX_PASSWORD_CHARS = 1024

    fun isEncrypted(bytes: ByteArray): Boolean {
        validateEnvelope(bytes)
        return bytes[magic.size].toInt() == 1
    }

    fun seal(
        archive: ByteArray,
        password: CharArray?,
    ): ByteArray {
        require(archive.size <= MAX_FILE_BYTES - HEADER_BYTES - TAG_BITS / Byte.SIZE_BITS)
        if (password == null) return header(false) + archive
        require(password.size in MIN_PASSWORD_CHARS..MAX_PASSWORD_CHARS) {
            "Пароль должен содержать от 10 до 1024 символов"
        }
        val header = header(true)
        val cipher = cipher(Cipher.ENCRYPT_MODE, header, password)
        return header + cipher.doFinal(archive)
    }

    fun open(
        bytes: ByteArray,
        password: CharArray?,
    ): ByteArray {
        val encrypted = isEncrypted(bytes)
        if (!encrypted) return bytes.copyOfRange(HEADER_BYTES, bytes.size)
        if (password == null || password.isEmpty()) throw BackupPasswordException()
        val header = bytes.copyOfRange(0, HEADER_BYTES)
        return try {
            cipher(Cipher.DECRYPT_MODE, header, password).doFinal(bytes, HEADER_BYTES, bytes.size - HEADER_BYTES)
        } catch (failure: GeneralSecurityException) {
            throw BackupPasswordException(failure)
        }
    }

    private fun header(encrypted: Boolean): ByteArray =
        ByteBuffer
            .allocate(HEADER_BYTES)
            .apply {
                put(magic)
                put(if (encrypted) 1.toByte() else 0.toByte())
                putInt(if (encrypted) ITERATIONS else 0)
                put(ByteArray(SALT_BYTES + NONCE_BYTES).also { if (encrypted) SecureRandom().nextBytes(it) })
            }.array()

    private fun validateEnvelope(bytes: ByteArray) {
        require(bytes.size in (HEADER_BYTES + 1)..MAX_FILE_BYTES) { "Неподдерживаемая или повреждённая копия" }
        require(bytes.copyOfRange(0, magic.size).contentEquals(magic)) { "Старый формат копии не поддерживается" }
        val mode = bytes[magic.size].toInt()
        require(mode == 0 || mode == 1)
        val iterations = ByteBuffer.wrap(bytes, magic.size + 1, Int.SIZE_BYTES).int
        require(iterations == if (mode == 1) ITERATIONS else 0)
        if (mode == 0) require(bytes.copyOfRange(SALT_OFFSET, HEADER_BYTES).all { it == 0.toByte() })
    }

    private fun cipher(
        mode: Int,
        header: ByteArray,
        password: CharArray,
    ): Cipher {
        require(password.size <= MAX_PASSWORD_CHARS)
        val specification = PBEKeySpec(password, header.copyOfRange(SALT_OFFSET, NONCE_OFFSET), ITERATIONS, KEY_BITS)
        val key =
            try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).encoded
            } finally {
                specification.clearPassword()
            }
        return try {
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                val parameters = GCMParameterSpec(TAG_BITS, header.copyOfRange(NONCE_OFFSET, HEADER_BYTES))
                init(mode, SecretKeySpec(key, "AES"), parameters)
                updateAAD(header)
            }
        } finally {
            key.fill(0)
        }
    }
}

class BackupPasswordException(
    cause: Throwable? = null,
) : IllegalArgumentException("Неверный пароль или повреждённая защищённая копия", cause)
