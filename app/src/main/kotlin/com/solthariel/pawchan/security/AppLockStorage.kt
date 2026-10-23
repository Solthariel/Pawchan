package com.solthariel.pawchan.security

import com.solthariel.pawchan.domain.security.AppLockDelay
import javax.crypto.Cipher

/** App-owned storage boundary; the ViewModel does not own Android preferences or Keystore. */
internal interface AppLockStorage {
    val fingerprintAvailable: Boolean

    suspend fun read(): LockConfiguration

    suspend fun setPin(pin: String)

    suspend fun verify(pin: String): PinVerification

    suspend fun retryRemainingMillis(): Long

    suspend fun resetAttempts()

    suspend fun setBiometric(enabled: Boolean)

    suspend fun setDelay(delay: AppLockDelay)

    suspend fun clear()

    suspend fun biometricCipher(recreate: Boolean = false): Cipher
}
