package com.solthariel.pawchan.security

import com.solthariel.pawchan.domain.security.AppLockDelay
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PinOperationCancellationTest {
    @Test fun busyRejectsDuplicatePinAndScreenOffCancelsPendingUnlock() =
        withLockModel(enabled = true) { model, store ->
            val result = CompletableDeferred<PinVerification>()
            store.onVerify = { result.await() }
            model.enterPin("2468")
            model.enterPin("2468")
            assertEquals(1, store.verifies)
            assertTrue(model.state.value.busy)
            model.lifecycle.screenOff()
            result.complete(PinVerification.VALID)
            assertTrue(model.state.value.locked)
            assertFalse(model.state.value.busy)
            store.onVerify = null
            model.enterPin("2468")
            assertFalse(model.state.value.locked)
        }

    @Test fun immediateBackgroundCancelsCurrentPinEditorVerification() =
        withLockModel(enabled = true) { model, store ->
            model.enterPin("2468")
            model.setDelay(AppLockDelay.IMMEDIATE)
            model.edit(LockAction.CHANGE)
            val result = CompletableDeferred<PinVerification>()
            store.onVerify = { result.await() }
            model.enterPin("2468")
            model.lifecycle.background()
            result.complete(PinVerification.VALID)
            assertTrue(model.state.value.locked)
            assertFalse(model.state.value.busy)
            assertNull(model.state.value.editor)
        }

    @Test fun lateCancellationCleanupCannotClearTheNextOperationsBusyFlag() =
        withLockModel(enabled = true) { model, store ->
            val first = CompletableDeferred<PinVerification>()
            val cleanup = CompletableDeferred<Unit>()
            store.onVerify = {
                try {
                    first.await()
                } finally {
                    withContext(NonCancellable) { cleanup.await() }
                }
            }
            model.enterPin("2468")
            model.lifecycle.screenOff()
            val next = CompletableDeferred<PinVerification>()
            store.onVerify = { next.await() }
            model.enterPin("2468")
            assertTrue(model.state.value.busy)
            cleanup.complete(Unit)
            assertTrue(model.state.value.busy)
            assertTrue(model.state.value.locked)
            next.complete(PinVerification.VALID)
            assertFalse(model.state.value.busy)
            assertFalse(model.state.value.locked)
        }

    @Test fun lateCancelledStorageFailureCannotOverwriteTheNextAttempt() =
        withLockModel(enabled = true) { model, store ->
            val first = CompletableDeferred<PinVerification>()
            val cleanup = CompletableDeferred<Unit>()
            store.onVerify = {
                try {
                    first.await()
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    withContext(NonCancellable) { cleanup.await() }
                    throw java.io.IOException("late fixture failure", cancelled)
                }
            }
            model.enterPin("2468")
            model.lifecycle.screenOff()
            val next = CompletableDeferred<PinVerification>()
            store.onVerify = { next.await() }
            model.enterPin("2468")
            cleanup.complete(Unit)
            assertTrue(model.state.value.busy)
            assertNull(model.state.value.error)
            next.complete(PinVerification.VALID)
            assertFalse(model.state.value.locked)
        }
}
