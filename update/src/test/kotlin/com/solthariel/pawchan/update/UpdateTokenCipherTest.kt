package com.solthariel.pawchan.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test
import javax.crypto.KeyGenerator

class UpdateTokenCipherTest {
    private val key = KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
    private val cipher = UpdateTokenCipher(key, "com.solthariel.pawchan.dev")
    private val token = "github_pat_example_test_credential_only"

    @Test fun roundTripDoesNotStorePlaintextAndUsesFreshNonces() {
        val first = cipher.encrypt(token)
        val second = cipher.encrypt(token)
        assertEquals(token, cipher.decrypt(first))
        assertFalse(first.contentEquals(second))
        assertFalse(String(first, Charsets.ISO_8859_1).contains(token))
    }

    @Test fun tamperingAndAnotherApplicationIdCannotDecrypt() {
        val data = cipher.encrypt(token)
        data[data.lastIndex] = (data.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) { cipher.decrypt(data) }
        assertThrows(Exception::class.java) { UpdateTokenCipher(key, "another.app").decrypt(cipher.encrypt(token)) }
    }

    @Test fun malformedFilesAndTokensAreRejected() {
        listOf(byteArrayOf(), ByteArray(3), ByteArray(MAX_CIPHERTEXT_BYTES + 1)).forEach {
            assertThrows(IllegalArgumentException::class.java) { cipher.decrypt(it) }
        }
        listOf("short", token + "\n", "т".repeat(40), "x".repeat(256)).forEach {
            assertFalse(validUpdateToken(it))
            assertThrows(IllegalArgumentException::class.java) { cipher.encrypt(it) }
        }
    }
}
