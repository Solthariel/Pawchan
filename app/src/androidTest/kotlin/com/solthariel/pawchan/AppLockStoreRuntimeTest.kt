package com.solthariel.pawchan

import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.domain.security.AppLockDelay
import com.solthariel.pawchan.security.AppLockStore
import com.solthariel.pawchan.security.PinVerification
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Requires a real Android Keystore; never substitutes a JVM fake for device security. */
class AppLockStoreRuntimeTest {
    @Test fun pinPersistsWithoutPlaintextAndCanBeChangedAndRemoved() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val store = AppLockStore(context, "app_lock_runtime_test")
            store.clear()
            try {
                store.setPin("1840")
                assertTrue(store.read().enabled)
                val reopened = AppLockStore(context, "app_lock_runtime_test")
                assertEquals(PinVerification.VALID, reopened.verify("1840"))
                assertEquals(PinVerification.INVALID, reopened.verify("0000"))
                assertFalse(
                    context
                        .getSharedPreferences("app_lock_runtime_test", 0)
                        .all.values
                        .any { it == "1840" },
                )
                reopened.setPin("5829")
                assertEquals(PinVerification.INVALID, reopened.verify("1840"))
                assertEquals(PinVerification.VALID, reopened.verify("5829"))
                reopened.clear()
                assertFalse(reopened.read().enabled)
            } finally {
                store.clear()
            }
        }

    @Test fun fiveWrongPinsThrottleEvenAfterReopeningTheStore() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val store = AppLockStore(context, "app_lock_runtime_test")
            store.clear()
            try {
                store.setPin("1840")
                repeat(4) { assertEquals(PinVerification.INVALID, store.verify("0000")) }
                assertEquals(PinVerification.WAIT, store.verify("0000"))
                assertEquals(PinVerification.WAIT, AppLockStore(context, "app_lock_runtime_test").verify("1840"))
            } finally {
                store.clear()
            }
        }

    @Test fun delayDefaultsToOneMinuteAndPersistsEverySupportedValue() =
        runBlocking {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val namespace = "app_lock_delay_runtime_test"
            val store = AppLockStore(context, namespace)
            store.clear()
            try {
                store.setPin("1840")
                assertEquals(AppLockDelay.MINUTE_1, store.read().delay)
                AppLockDelay.entries.forEach { delay ->
                    store.setDelay(delay)
                    val reopened = AppLockStore(context, namespace)
                    assertEquals(delay, reopened.read().delay)
                    assertEquals(PinVerification.VALID, reopened.verify("1840"))
                }
                context
                    .getSharedPreferences(namespace, 0)
                    .edit()
                    .putLong("lock_delay_millis", -1L)
                    .commit()
                assertEquals(AppLockDelay.MINUTE_1, AppLockStore(context, namespace).read().delay)
                store.setPin("5829")
                store.setDelay(AppLockDelay.HOUR_1)
                store.setPin("1840")
                assertEquals(AppLockDelay.HOUR_1, store.read().delay)
                store.clear()
                assertEquals(AppLockDelay.MINUTE_1, store.read().delay)
            } finally {
                store.clear()
            }
        }
}
