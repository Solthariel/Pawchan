package com.solthariel.pawchan.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/** PIN verification also requires the non-exportable installation key. */
internal class AppLockCrypto(
    namespace: String,
) {
    private val pinAlias = "$namespace.pin.pepper.v1"
    private val biometricAlias = "$namespace.biometric.v1"

    private fun store(): KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

    fun verifier(
        pin: String,
        salt: ByteArray,
    ): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, DERIVED_KEY_BITS)
        val derived =
            try {
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            } finally {
                spec.clearPassword()
            }
        return try {
            Mac.getInstance("HmacSHA256").run {
                init(pinKey())
                doFinal(derived)
            }
        } finally {
            derived.fill(0)
        }
    }

    private fun pinKey(): SecretKey =
        (store().getKey(pinAlias, null) as? SecretKey) ?: KeyGenerator
            .getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
            .run {
                init(
                    KeyGenParameterSpec
                        .Builder(pinAlias, KeyProperties.PURPOSE_SIGN)
                        .setDigests(KeyProperties.DIGEST_SHA256)
                        .build(),
                )
                generateKey()
            }

    fun biometricCipher(recreate: Boolean): Cipher {
        if (recreate) clearBiometricKey()
        val key =
            (store().getKey(biometricAlias, null) as? SecretKey) ?: KeyGenerator
                .getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                .run {
                    init(
                        KeyGenParameterSpec
                            .Builder(biometricAlias, KeyProperties.PURPOSE_ENCRYPT)
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setUserAuthenticationRequired(true)
                            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
                            .setInvalidatedByBiometricEnrollment(true)
                            .build(),
                    )
                    generateKey()
                }
        return Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }
    }

    fun clearBiometricKey() {
        store().deleteEntry(biometricAlias)
    }
}

private const val ITERATIONS = 210_000

private const val DERIVED_KEY_BITS = 256
