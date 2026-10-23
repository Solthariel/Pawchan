package com.solthariel.pawchan.security

import com.solthariel.pawchan.domain.security.AppLockDelay
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockBoundaryTest {
    @Test fun publicAccessDecisionTracksUnlockAndImmediateRelock() =
        withLockModel(enabled = true) { session, _ ->
            assertFalse(session.isAccessAllowed())
            assertFalse(session.accessChanges.first())
            session.enterPin("2468")
            assertTrue(session.isAccessAllowed())
            assertTrue(session.accessChanges.first())
            session.lifecycle.screenOff()
            assertFalse(session.isAccessAllowed())
            assertFalse(session.accessChanges.first())
        }

    @Test fun unsuccessfulPinNeverGrantsMediaAccess() =
        withLockModel(enabled = true) { session, _ ->
            session.enterPin("0000")
            assertFalse(session.isAccessAllowed())
            assertFalse(session.accessChanges.first())
        }

    @Test fun cancellationOfUnlockKeepsThePublicAccessDecisionDenied() =
        withLockModel(enabled = true) { session, store ->
            val pending = kotlinx.coroutines.CompletableDeferred<PinVerification>()
            store.onVerify = { pending.await() }
            session.enterPin("2468")
            session.lifecycle.screenOff()
            pending.complete(PinVerification.VALID)
            assertFalse(session.isAccessAllowed())
            assertFalse(session.accessChanges.first())
        }

    @Test fun settingsProjectionRetainsConfigurationAndHidesEditorPayload() {
        val first =
            AppLockState(
                ready = true,
                configuration = LockConfiguration(enabled = true, biometric = true, delay = AppLockDelay.SECONDS_30),
                locked = false,
                error = "same error",
                editor = LockEditor(LockAction.CHANGE, PinStage.CONFIRM, "2468"),
            )
        val projection = first.settingsProjection()
        assertEquals(AppLockSettingsState(true, true, AppLockDelay.SECONDS_30, true, "same error"), projection)
        assertEquals(projection, first.copy(editor = first.editor?.copy(first = "1357")).settingsProjection())
        assertFalse(first.copy(editor = null).settingsProjection().editing)
    }
}
