package com.solthariel.pawchan.security

import androidx.lifecycle.ViewModelStore
import com.solthariel.pawchan.domain.security.AppLockDelay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import javax.crypto.Cipher

internal class FakeLockStorage(
    enabled: Boolean,
) : AppLockStorage {
    var configuration = LockConfiguration(enabled = enabled)
    var pin = "2468"
    var verifies = 0
    var onVerify: (suspend (String) -> PinVerification)? = null
    override val fingerprintAvailable = false

    override suspend fun read() = configuration

    override suspend fun setPin(pin: String) {
        this.pin = pin
        configuration = configuration.copy(enabled = true)
    }

    override suspend fun verify(pin: String): PinVerification {
        verifies++
        return onVerify?.invoke(pin) ?: if (pin == this.pin) PinVerification.VALID else PinVerification.INVALID
    }

    override suspend fun retryRemainingMillis() = 0L

    override suspend fun resetAttempts() = Unit

    override suspend fun setBiometric(enabled: Boolean) {
        configuration = configuration.copy(biometric = enabled)
    }

    override suspend fun setDelay(delay: AppLockDelay) {
        configuration = configuration.copy(delay = delay)
    }

    override suspend fun clear() {
        configuration = LockConfiguration()
    }

    override suspend fun biometricCipher(recreate: Boolean): Cipher = error("Biometrics not used by this fixture")
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
internal fun withLockModel(
    enabled: Boolean = false,
    block: suspend (AppLockViewModel, FakeLockStorage) -> Unit,
) = runTest {
    Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
    val owner = ViewModelStore()
    try {
        val store = FakeLockStorage(enabled)
        val model = AppLockViewModel(store) { testScheduler.currentTime }
        owner.put("lock", model)
        block(model, store)
    } finally {
        owner.clear()
        Dispatchers.resetMain()
    }
}
