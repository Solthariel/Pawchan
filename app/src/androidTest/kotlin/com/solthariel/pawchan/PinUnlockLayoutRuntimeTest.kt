package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.security.AppLockState
import com.solthariel.pawchan.security.LockConfiguration
import com.solthariel.pawchan.security.PinUnlockContent
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PinUnlockLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val state =
        mutableStateOf(
            AppLockState(
                ready = true,
                configuration = LockConfiguration(enabled = true, biometric = true),
            ),
        )

    @Test fun statusChangesNeverMoveTitleDotsKeysOrFingerprintButton() {
        show()
        val before = bounds()
        listOf(
            state.value.copy(error = "Неверный PIN-код"),
            state.value.copy(error = "Введите PIN или используйте отпечаток"),
            state.value.copy(busy = true, error = "Неверный PIN-код"),
            state.value.copy(ready = false),
            state.value,
        ).forEach { next ->
            compose.runOnIdle { state.value = next }
            compose.waitForIdle()
            assertEquals(before, bounds())
        }
        val area = compose.onNodeWithTag("unlock-area").fetchSemanticsNode().boundsInRoot
        val content = compose.onNodeWithTag("pin-unlock-content").fetchSemanticsNode().boundsInRoot
        assertEquals(area.center.y, content.center.y, 1f)
    }

    @Test fun verificationReplacesErrorAndDisablesInputRatherThanAddingAnotherRow() {
        state.value = state.value.copy(busy = true, error = "Неверный PIN-код")
        show()
        compose.onNodeWithText("Неверный PIN-код").assertDoesNotExist()
        compose.onNodeWithText("1").assertIsNotEnabled()
        compose.onNodeWithContentDescription("Отпечаток пальца").assertIsNotEnabled()
        compose.onNodeWithText("Проверка…").fetchSemanticsNode()
    }

    @Test fun fourDigitsSubmitWithoutConfirmationAndDeletingRestoresFingerprint() {
        var confirmed: String? = null
        var fingerprints = 0
        compose.setContent {
            MaterialTheme { PinUnlockContent(state.value, true, { confirmed = it }, { fingerprints++ }) }
        }
        compose.onNodeWithContentDescription("Отпечаток пальца").performClick()
        compose.runOnIdle { assertEquals(1, fingerprints) }
        val original = compose.onNodeWithTag("pin-unlock-action").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("1").performClick()
        compose.onNodeWithContentDescription("Подтвердить PIN-код").assertDoesNotExist()
        compose.onNodeWithContentDescription("Удалить цифру").performClick()
        compose.onNodeWithContentDescription("Отпечаток пальца").assertIsEnabled()
        listOf("1", "2", "3", "4").forEach { compose.onNodeWithText(it).performClick() }
        compose.runOnIdle { assertEquals("1234", confirmed) }
        assertEquals(original, compose.onNodeWithTag("pin-unlock-action").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Подтвердить PIN-код").assertDoesNotExist()
    }

    @Test fun unavailableBiometricsNeverExposeFingerprintAction() {
        compose.setContent { MaterialTheme { PinUnlockContent(state.value, false, {}, {}) } }
        compose.onNodeWithContentDescription("Отпечаток пальца").assertDoesNotExist()
        compose.onNodeWithContentDescription("Подтвердить PIN-код").assertDoesNotExist()
    }

    @Test fun largeFontAndCompactHeightKeepActionsReachable() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme {
                    Box(Modifier.size(360.dp, 320.dp).testTag("unlock-area")) {
                        PinUnlockContent(state.value, true, {}, {})
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Отпечаток пальца").performScrollTo().assertIsDisplayed()
        val area = compose.onNodeWithTag("unlock-area").fetchSemanticsNode().boundsInRoot
        val action = compose.onNodeWithTag("pin-unlock-action").fetchSemanticsNode().boundsInRoot
        org.junit.Assert.assertTrue(action.left >= area.left && action.right <= area.right)
    }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(360.dp, 720.dp).testTag("unlock-area")) {
                    PinUnlockContent(state.value, true, {}, {})
                }
            }
        }
    }

    private fun bounds() =
        listOf(
            compose.onNodeWithText("Введите PIN-код"),
            compose.onNodeWithContentDescription("Введено цифр: 0 из 4"),
            compose.onNodeWithText("1"),
            compose.onNodeWithText("0"),
            compose.onNodeWithContentDescription("Отпечаток пальца"),
        ).map { it.fetchSemanticsNode().boundsInRoot }
}
