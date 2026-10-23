package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.security.AppLockState
import com.solthariel.pawchan.security.LockAction
import com.solthariel.pawchan.security.LockEditor
import com.solthariel.pawchan.security.PinPad
import com.solthariel.pawchan.security.PinStage
import com.solthariel.pawchan.security.PinUnlockContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PinEditorInputRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val state =
        mutableStateOf(
            AppLockState(ready = true, locked = false, editor = LockEditor(LockAction.ENABLE, PinStage.NEW)),
        )

    @Test fun confirmationAcceptsDigitsAfterFastStageTransition() {
        val submitted = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                PinPad("PIN", state.value) {
                    submitted.add(it)
                    state.value = state.value.copy(editor = state.value.editor?.copy(stage = PinStage.CONFIRM))
                }
            }
        }
        type("1234")
        compose.onNodeWithText("5").performClick()
        compose.onNodeWithContentDescription("Введено цифр: 1 из 4").assertIsDisplayed()
        type("678")
        compose.runOnIdle { assertEquals(listOf("1234", "5678"), submitted) }
    }

    @Test fun identicalErrorsAndUnobservedBusyPulseStillPermitThirdAttempt() {
        var attempts = 0
        compose.setContent {
            MaterialTheme {
                PinUnlockContent(state.value, false, {
                    attempts++
                    state.value =
                        state.value.copy(
                            busy = true,
                            pinInput = state.value.pinInput.copy(epoch = state.value.pinInputEpoch + 1),
                        )
                    state.value = state.value.copy(busy = false, error = "Неверный PIN-код")
                }, {})
            }
        }
        repeat(3) { type("1234") }
        compose.runOnIdle { assertEquals(3, attempts) }
    }

    @Test fun editorKeysAndDeleteUseCurrentDigitsWithinOneFrame() {
        val submitted = mutableListOf<String>()
        compose.setContent { MaterialTheme { PinPad("PIN", state.value, submitted::add) } }
        clickInOneFrame(listOf("1", "2", "⌫", "3", "4", "5"))
        compose.runOnIdle { assertEquals(listOf("1345"), submitted) }
    }

    @Test fun unlockKeysUseCurrentDigitsAndSubmitOnlyOnceWithinOneFrame() {
        val submitted = mutableListOf<String>()
        compose.setContent { MaterialTheme { PinUnlockContent(state.value, false, submitted::add, {}) } }
        clickInOneFrame(listOf("1", "2", "3", "4", "5"))
        compose.runOnIdle { assertEquals(listOf("1234"), submitted) }
    }

    @Test fun mismatchBackToNewDoesNotKeepTheSubmissionClaim() {
        val submitted = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                PinPad("PIN", state.value) {
                    submitted.add(it)
                    val stage = if (state.value.editor?.stage == PinStage.NEW) PinStage.CONFIRM else PinStage.NEW
                    state.value = state.value.copy(editor = state.value.editor?.copy(stage = stage))
                }
            }
        }
        type("1234")
        type("1235")
        type("6789")
        compose.runOnIdle { assertEquals(listOf("1234", "1235", "6789"), submitted) }
    }

    private fun type(value: String) {
        value.forEach { compose.onNodeWithText(it.toString()).performClick() }
    }

    private fun clickInOneFrame(keys: List<String>) {
        val clicks =
            keys.map {
                compose
                    .onNodeWithText(it)
                    .fetchSemanticsNode()
                    .config[SemanticsActions.OnClick]
                    .action!!
            }
        compose.runOnIdle { clicks.forEach { it() } }
        compose.waitForIdle()
    }
}
