package com.solthariel.pawchan.security

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricManager
import android.os.SystemClock
import androidx.core.content.edit
import com.solthariel.pawchan.domain.security.AppLockDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** Separate from portable backups; no plaintext PIN, not even in the saved UI state. */
internal class AppLockStore(
    context: Context,
    namespace: String = "app_lock",
    private val nowMillis: () -> Long = SystemClock::elapsedRealtime,
) : AppLockStorage {
    private val preferences = context.getSharedPreferences(namespace, Context.MODE_PRIVATE)
    private val crypto = AppLockCrypto(namespace)
    private val biometricManager = context.getSystemService(BiometricManager::class.java)
    private val fingerprintHardware = context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    override val fingerprintAvailable get() =
        fingerprintHardware &&
            biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

    override suspend fun read(): LockConfiguration =
        withContext(Dispatchers.IO) {
            LockConfiguration(
                preferences.contains("verifier"),
                preferences.getBoolean("biometric", false),
                AppLockDelay.fromMillis(preferences.getLong("lock_delay_millis", AppLockDelay.MINUTE_1.millis)),
            )
        }

    override suspend fun setPin(pin: String) =
        withContext(Dispatchers.IO) {
            require(pin.length == PIN_LENGTH && pin.all { it in '0'..'9' })
            val salt = ByteArray(PIN_SALT_BYTES).also(SecureRandom()::nextBytes)
            val verifier = crypto.verifier(pin, salt)
            commit {
                putString("salt", encode(salt))
                putString("verifier", encode(verifier))
                putInt("failures", 0)
                putLong("retry_at", 0L)
            }
        }

    override suspend fun verify(pin: String): PinVerification =
        withContext(Dispatchers.IO) {
            val now = nowMillis()
            val deadline = preferences.getLong("retry_at", 0L)
            // A reboot resets elapsedRealtime. Never accept an old deadline greater than the maximum backoff.
            if (PinAttemptPolicy.remaining(now, deadline) > 0) return@withContext PinVerification.WAIT
            val salt = decode(requireNotNull(preferences.getString("salt", null)))
            val expected = decode(requireNotNull(preferences.getString("verifier", null)))
            val valid = MessageDigest.isEqual(expected, crypto.verifier(pin, salt))
            val failures = PinAttemptPolicy.nextFailures(preferences.getInt("failures", 0), valid)
            commit {
                putInt("failures", failures)
                val retryAt =
                    if (PinAttemptPolicy.shouldDelay(failures)) nowMillis() + PinAttemptPolicy.DELAY_MILLIS else 0L
                putLong("retry_at", retryAt)
            }
            if (valid) {
                PinVerification.VALID
            } else if (PinAttemptPolicy.shouldDelay(failures)) {
                PinVerification.WAIT
            } else {
                PinVerification.INVALID
            }
        }

    override suspend fun retryRemainingMillis(): Long =
        withContext(Dispatchers.IO) {
            PinAttemptPolicy.remaining(nowMillis(), preferences.getLong("retry_at", 0L))
        }

    override suspend fun resetAttempts() =
        withContext(Dispatchers.IO) {
            commit {
                putInt("failures", 0)
                putLong("retry_at", 0L)
            }
        }

    override suspend fun setBiometric(enabled: Boolean) =
        withContext(Dispatchers.IO) {
            commit { putBoolean("biometric", enabled) }
        }

    override suspend fun setDelay(delay: AppLockDelay) =
        withContext(Dispatchers.IO) {
            commit { putLong("lock_delay_millis", delay.millis) }
        }

    override suspend fun clear() =
        withContext(Dispatchers.IO) {
            commit { clear() }
            crypto.clearBiometricKey()
        }

    private fun commit(update: SharedPreferences.Editor.() -> Unit) {
        preferences.edit {
            update()
            // Check durability before returning. KTX's subsequent apply has no pending changes.
            check(commit()) { "Could not persist app lock configuration" }
        }
    }

    override suspend fun biometricCipher(recreate: Boolean) =
        withContext(Dispatchers.IO) {
            crypto.biometricCipher(recreate)
        }
}

internal data class LockConfiguration(
    val enabled: Boolean = false,
    val biometric: Boolean = false,
    val delay: AppLockDelay = AppLockDelay.MINUTE_1,
)

internal enum class PinVerification { VALID, INVALID, WAIT }

internal const val PIN_LENGTH = 4

private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)

private fun decode(value: String): ByteArray = Base64.getDecoder().decode(value)

private const val PIN_SALT_BYTES = 16
