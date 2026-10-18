package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.security.AppLockDelay
import com.solthariel.pawchan.security.AppLockState
import com.solthariel.pawchan.security.LockConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockConfigurationTest {
    @Test fun onlyReadyUnlockedIdleStateAllowsEditing() {
        val editable = AppLockState(ready = true, locked = false)
        assertTrue(editable.canEdit)
        assertFalse(editable.copy(ready = false).canEdit)
        assertFalse(editable.copy(locked = true).canEdit)
        assertFalse(editable.copy(busy = true).canEdit)
        assertFalse(AppLockState().canEdit)
    }

    @Test fun freshConfigurationUsesOneMinuteWithoutEnablingProtection() {
        val configuration = LockConfiguration()
        assertEquals(AppLockDelay.MINUTE_1, configuration.delay)
        assertFalse(configuration.enabled)
        assertFalse(configuration.biometric)
    }
}
