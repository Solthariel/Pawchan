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
                owner.externalSelection.begin()
                owner.background()
                runCurrent()
                advanceTimeBy(1_000)
                owner.externalSelection.returned()
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
            owner.externalSelection.begin()
            owner.background()
            owner.foreground()
            owner.finishForeground()
            assertEquals(3, locks)
        }

    @Test fun screenOffRevokesThePickerAndLeaseExpiresWhileStillOutside() =
        runTest {
            var locks = 0
            val owner = AppLockLifecycle(backgroundScope, { testScheduler.currentTime }, { unlocked }) { locks++ }
            owner.externalSelection.begin()
            owner.background()
            owner.screenOff()
            assertEquals(1, locks)
            owner.externalSelection.returned()
            owner.externalSelection.begin()
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
            owner.externalSelection.begin()
            owner.externalSelection.returned()
            owner.finishForeground()
            owner.background()
            assertEquals(1, locks)
        }

    @Test fun lockedAndUnreadySessionsCannotBeginAnExternalSelectionLease() =
        runTest {
            for (state in listOf(unlocked.copy(locked = true), unlocked.copy(ready = false))) {
                var locks = 0
                val owner = AppLockLifecycle(backgroundScope, { testScheduler.currentTime }, { state }) { locks++ }
                owner.externalSelection.begin()
                owner.background()
                owner.externalSelection.returned()
                owner.foreground()
                owner.finishForeground()
                assertEquals(2, locks)
                owner.close()
            }
        }

    @Test fun cancelledExternalSelectionCannotGrantAnException() =
        runTest {
            var locks = 0
            val owner = AppLockLifecycle(backgroundScope, { testScheduler.currentTime }, { unlocked }) { locks++ }
            owner.externalSelection.begin()
            owner.externalSelection.cancel()
            owner.background()
            owner.foreground()
            owner.finishForeground()
            assertEquals(2, locks)
            owner.close()
        }
}
