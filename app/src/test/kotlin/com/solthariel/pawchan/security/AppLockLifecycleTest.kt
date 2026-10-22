package com.solthariel.pawchan.security

import com.solthariel.pawchan.domain.security.AppLockDelay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AppLockLifecycleTest {
    private val unlocked =
        AppLockState(
            ready = true,
            configuration =
                LockConfiguration(
                    enabled = true,
                    delay = AppLockDelay.IMMEDIATE,
                ),
            locked = false,
        )

    @Test fun pickerResultOrCancellationReturnsWithoutAnImmediateRelock() =
        runTest {
            var locks = 0
            val owner = AppLockLifecycle(backgroundScope, { testScheduler.currentTime }, { unlocked }) { locks++ }
            repeat(2) {
                owner.pickerHost.begin()
                owner.background()
                runCurrent()
                advanceTimeBy(1_000)
                owner.pickerHost.returned()
                owner.foreground()
                owner.finishForeground()
                assertEquals(0, locks)
            }
        }

    @Test fun normalBackgroundAndHomeWithoutAResultStillLock() =
        runTest {
            var locks = 0
            val owner = AppLockLifecycle(backgroundScope, { testScheduler.currentTime }, { unlocked }) { locks++ }
            owner.background()
            assertEquals(1, locks)
            owner.foreground()
            owner.pickerHost.begin()
            owner.background()
            owner.foreground()
            owner.finishForeground()
            assertEquals(3, locks)
        }

    @Test fun screenOffRevokesThePickerAndLeaseExpiresWhileStillOutside() =
        runTest {
            var locks = 0
            val owner = AppLockLifecycle(backgroundScope, { testScheduler.currentTime }, { unlocked }) { locks++ }
            owner.pickerHost.begin()
            owner.background()
            owner.screenOff()
            assertEquals(1, locks)
            owner.pickerHost.returned()
            owner.pickerHost.begin()
            owner.background()
            runCurrent()
            advanceTimeBy(PICKER_LEASE_MILLIS)
            runCurrent()
            assertEquals(2, locks)
            owner.close()
        }

    @Test fun chooserWithoutBackgroundCannotBypassTheNextRealBackground() =
        runTest {
            var locks = 0
            val owner = AppLockLifecycle(backgroundScope, { testScheduler.currentTime }, { unlocked }) { locks++ }
            owner.pickerHost.begin()
            owner.pickerHost.returned()
            owner.finishForeground()
            owner.background()
            assertEquals(1, locks)
        }
}
