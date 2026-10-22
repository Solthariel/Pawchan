package com.solthariel.pawchan.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PickerLockGateTest {
    @Test fun onlyOneMatchingRoundTripIsExempt() {
        val gate = PickerLockGate { 0L }
        gate.begin()
        assertTrue(gate.deferBackground())
        gate.returned()
        assertTrue(gate.finish())
        assertFalse(gate.finish())
        assertFalse(gate.deferBackground())
    }

    @Test fun reopeningFromHomeWithoutAResultNeverGrantsAccess() {
        val gate = PickerLockGate { 0L }
        gate.begin()
        assertTrue(gate.deferBackground())
        assertFalse(gate.finish())
    }

    @Test fun screenOffAndFreshProcessRevokeEvenALateResult() {
        val gate = PickerLockGate { 0L }
        gate.begin()
        gate.deferBackground()
        gate.revoke()
        gate.returned()
        assertFalse(gate.finish())
        assertFalse(PickerLockGate { 0L }.finish())
    }

    @Test fun expiredAndBackwardsClockNeverGrantAccess() {
        var now = 100L
        val gate = PickerLockGate { now }
        gate.begin()
        gate.deferBackground()
        gate.returned()
        now += PICKER_LEASE_MILLIS + 1
        assertFalse(gate.finish())
        gate.begin()
        now--
        assertFalse(gate.deferBackground())
    }

    @Test fun resultWithoutAStopCannotExemptSomeLaterBackgroundTrip() {
        val gate = PickerLockGate { 0L }
        gate.begin()
        gate.returned()
        assertFalse(gate.deferBackground())
        assertFalse(gate.finish())
    }
}
