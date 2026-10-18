package com.solthariel.pawchan.data.maintenance

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupProtectionTest {
    private val password = "correct horse battery staple".toCharArray()
    private val payload = "private history and posts".toByteArray()

    @Test fun encryptedRoundTripAndRandomizedCiphertext() {
        val first = BackupProtection.seal(payload, password)
        val second = BackupProtection.seal(payload, password)
        assertTrue(BackupProtection.isEncrypted(first))
        assertFalse(first.contentEquals(second))
        assertFalse(first.toString(Charsets.ISO_8859_1).contains("private history"))
        assertArrayEquals(payload, BackupProtection.open(first, password))
    }

    @Test(expected = BackupPasswordException::class)
    fun wrongPasswordIsRejected() {
        BackupProtection.open(BackupProtection.seal(payload, password), "different password".toCharArray())
    }

    @Test(expected = BackupPasswordException::class)
    fun missingPasswordIsRejected() {
        BackupProtection.open(BackupProtection.seal(payload, password), null)
    }

    @Test(expected = BackupPasswordException::class)
    fun ciphertextModificationIsRejected() {
        val bytes = BackupProtection.seal(payload, password)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        BackupProtection.open(bytes, password)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unboundedKdfParametersAreRejectedBeforeDerivation() {
        val bytes = BackupProtection.seal(payload, password)
        bytes[8] = 127
        BackupProtection.open(bytes, password)
    }

    @Test fun unprotectedCopyHasExplicitEnvelope() {
        val bytes = BackupProtection.seal(payload, null)
        assertFalse(BackupProtection.isEncrypted(bytes))
        assertArrayEquals(payload, BackupProtection.open(bytes, null))
    }

    @Test(expected = IllegalArgumentException::class)
    fun oldPropertiesCopyIsNotAccepted() {
        BackupProtection.open("format=pawchan-client\nversion=2\nbookmarks=old".toByteArray(), null)
    }

    @Test(expected = IllegalArgumentException::class)
    fun shortExportPasswordIsRejected() {
        BackupProtection.seal(payload, "1234".toCharArray())
    }
}
