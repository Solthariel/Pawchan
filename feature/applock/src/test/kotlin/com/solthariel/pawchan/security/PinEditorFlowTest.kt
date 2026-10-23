package com.solthariel.pawchan.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class PinEditorFlowTest {
    @Test fun enableAcceptsNewPinAndConfirmationAsSeparateAttempts() =
        withLockModel { model, store ->
            model.edit(LockAction.ENABLE)
            val before = model.state.value.pinInputEpoch
            model.enterPin("1357")
            assertEquals(
                PinStage.CONFIRM,
                model.state.value.editor
                    ?.stage,
            )
            assertTrue(model.state.value.pinInputEpoch > before)
            assertFalse(model.state.value.busy)
            model.enterPin("1357")
            assertNull(model.state.value.editor)
            assertTrue(model.state.value.configuration.enabled)
            assertFalse(model.state.value.locked)
            assertEquals("1357", store.pin)
        }

    @Test fun changingPinRequiresCurrentThenNewThenConfirmation() =
        withLockModel(enabled = true) { model, store ->
            model.enterPin("2468")
            model.edit(LockAction.CHANGE)
            assertEquals(
                PinStage.CURRENT,
                model.state.value.editor
                    ?.stage,
            )
            model.enterPin("2468")
            assertEquals(
                PinStage.NEW,
                model.state.value.editor
                    ?.stage,
            )
            model.enterPin("1357")
            assertEquals(
                PinStage.CONFIRM,
                model.state.value.editor
                    ?.stage,
            )
            assertEquals("2468", store.pin)
            model.enterPin("1357")
            assertEquals("1357", store.pin)
            assertNull(model.state.value.editor)
        }

    @Test fun mismatchCanBeRetriedWithoutPersistingTheUnconfirmedPin() =
        withLockModel { model, store ->
            model.edit(LockAction.ENABLE)
            repeat(2) {
                model.enterPin("1357")
                model.enterPin("1358")
                assertEquals(
                    PinStage.NEW,
                    model.state.value.editor
                        ?.stage,
                )
                assertEquals(
                    "",
                    model.state.value.editor
                        ?.first,
                )
                assertFalse(store.configuration.enabled)
            }
            model.enterPin("9876")
            model.enterPin("9876")
            assertEquals("9876", store.pin)
        }

    @Test fun repeatedIdenticalErrorsStillAdvanceTheInputAttempt() =
        withLockModel(enabled = true) { model, store ->
            model.enterPin("0000")
            val first = model.state.value
            model.enterPin("0000")
            val second = model.state.value
            assertEquals(first.error, second.error)
            assertEquals(first.busy, second.busy)
            assertTrue(second.pinInputEpoch > first.pinInputEpoch)
            assertEquals(2, store.verifies)
            model.enterPin("2468")
            assertFalse(model.state.value.locked)
        }

    @Test fun repeatedStorageErrorsDoNotPermanentlyDisableRetry() =
        withLockModel(enabled = true) { model, store ->
            store.onVerify = { throw IOException("fixture failure") }
            model.enterPin("2468")
            val first = model.state.value
            model.enterPin("2468")
            assertEquals(first.error, model.state.value.error)
            assertTrue(model.state.value.pinInputEpoch > first.pinInputEpoch)
            assertFalse(model.state.value.busy)
            store.onVerify = null
            model.enterPin("2468")
            assertFalse(model.state.value.locked)
        }

    @Test fun cancelConfirmationDiscardsFirstPinAndNextEditStartsAtNew() =
        withLockModel { model, store ->
            model.edit(LockAction.ENABLE)
            model.enterPin("1357")
            model.cancelEdit()
            assertNull(model.state.value.editor)
            assertFalse(store.configuration.enabled)
            model.edit(LockAction.ENABLE)
            assertEquals(
                PinStage.NEW,
                model.state.value.editor
                    ?.stage,
            )
            assertEquals(
                "",
                model.state.value.editor
                    ?.first,
            )
        }

    @Test fun incompleteAndNonAsciiPinDoNotStartAnOperation() =
        withLockModel(enabled = true) { model, store ->
            val epoch = model.state.value.pinInputEpoch
            listOf("", "123", "12345", "１２３４", "12a4").forEach(model::enterPin)
            assertEquals(0, store.verifies)
            assertEquals(epoch, model.state.value.pinInputEpoch)
        }
}
