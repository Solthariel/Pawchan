package com.solthariel.pawchan.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinUnlockActionTest {
    private val ready = AppLockState(ready = true)

    @Test fun emptyPinOffersFingerprintOnlyWhenConfiguredAndAvailable() {
        assertEquals(PinUnlockAction.FINGERPRINT, pinUnlockAction("", true))
        assertEquals(PinUnlockAction.CONFIRM, pinUnlockAction("", false))
        assertTrue(pinUnlockActionEnabled("", PinUnlockAction.FINGERPRINT, ready))
        assertFalse(pinUnlockActionEnabled("", PinUnlockAction.CONFIRM, ready))
    }

    @Test fun enteringAndDeletingPinSwitchesActionWithoutAutoSubmit() {
        assertEquals(PinUnlockAction.CONFIRM, pinUnlockAction("1", true))
        assertEquals(PinUnlockAction.CONFIRM, pinUnlockAction("1234", true))
        assertFalse(pinUnlockActionEnabled("123", PinUnlockAction.CONFIRM, ready))
        assertTrue(pinUnlockActionEnabled("1234", PinUnlockAction.CONFIRM, ready))
        assertEquals(PinUnlockAction.FINGERPRINT, pinUnlockAction("1".dropLast(1), true))
    }

    @Test fun loadingAndVerificationDisableBothActions() {
        listOf(ready.copy(ready = false), ready.copy(busy = true)).forEach { state ->
            assertFalse(pinUnlockActionEnabled("", PinUnlockAction.FINGERPRINT, state))
            assertFalse(pinUnlockActionEnabled("1234", PinUnlockAction.CONFIRM, state))
        }
    }
}
