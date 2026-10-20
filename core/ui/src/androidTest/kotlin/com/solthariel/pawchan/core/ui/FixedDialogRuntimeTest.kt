package com.solthariel.pawchan.core.ui

import android.graphics.Insets
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogWindowProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FixedDialogRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nestedQualityAndImeDoNotRecenterTheParentWindow() {
        val editing = mutableStateOf(false)
        lateinit var parentView: View
        compose.setContent {
            MaterialTheme {
                AppDialog({}) {
                    val view = LocalView.current
                    SideEffect { parentView = view }
                    Column(Modifier.height(300.dp).testTag("fixed-parent")) {
                        TextButton(onClick = { editing.value = true }) { Text("Качество") }
                    }
                }
                if (editing.value) NumberInputDialog("JPEG", 100, 1..100, { editing.value = false }) {}
            }
        }
        val before = compose.onNodeWithTag("fixed-parent").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Качество").performClick()
        compose.runOnIdle {
            val window = (parentView.parent as DialogWindowProvider).window
            assertEquals(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING,
                window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST,
            )
            val ime =
                WindowInsets
                    .Builder()
                    .setInsets(WindowInsets.Type.ime(), Insets.of(0, 0, 0, 900))
                    .setVisible(WindowInsets.Type.ime(), true)
                    .build()
            parentView.rootView.dispatchApplyWindowInsets(ime)
        }
        compose.waitForIdle()
        val after = compose.onNodeWithTag("fixed-parent").fetchSemanticsNode().boundsInRoot
        assertEquals(before, after)
        compose.onNodeWithTag("number-input").assertExists()
    }

    @Test fun resetIsLeftAndCancelPrecedesApplyAtTheRight() {
        compose.setContent {
            MaterialTheme {
                AppAlertDialog({}, title = { Text("Тест") }, text = {
                    DialogActions(
                        neutral = { TextButton({}, Modifier.testTag("reset")) { Text("Сбросить") } },
                        dismiss = { TextButton({}, Modifier.testTag("cancel")) { Text("Отмена") } },
                        confirm = { TextButton({}, Modifier.testTag("apply")) { Text("Применить") } },
                    )
                })
            }
        }
        val reset = compose.onNodeWithTag("reset").fetchSemanticsNode().boundsInRoot
        val cancel = compose.onNodeWithTag("cancel").fetchSemanticsNode().boundsInRoot
        val apply = compose.onNodeWithTag("apply").fetchSemanticsNode().boundsInRoot
        assertTrue(reset.right <= cancel.left)
        assertTrue(cancel.right <= apply.left)
        assertEquals(reset.top, apply.top, 1f)
        assertEquals(reset.height, apply.height, 1f)
    }

    @Test fun outsideTapStillDismissesAFixedViewportDialog() {
        val open = mutableStateOf(true)
        compose.setContent {
            MaterialTheme { if (open.value) AppAlertDialog({ open.value = false }, title = { Text("Закрыть") }) }
        }
        compose.onNodeWithTag("dialog-backdrop").performTouchInput { click(Offset(1f, 1f)) }
        compose.runOnIdle { assertEquals(false, open.value) }
    }

    @Test fun onlyForegroundChangesForSelectionAndDisabledNeverUsesTheAccent() {
        lateinit var on: UiOptionColors
        lateinit var off: UiOptionColors
        lateinit var disabled: UiOptionColors
        compose.setContent {
            MaterialTheme {
                val active = uiOptionColors(true, true)
                val ordinary = uiOptionColors(false, true)
                val unavailable = uiOptionColors(true, false)
                SideEffect {
                    on = active
                    off = ordinary
                    disabled = unavailable
                }
                Text("Цвета")
            }
        }
        compose.runOnIdle {
            assertEquals(on.background, off.background)
            assertEquals(on.background, disabled.background)
            assertEquals(on.caption, off.caption)
            assertNotEquals(on.foreground, off.foreground)
            assertNotEquals(on.foreground, disabled.foreground)
        }
    }
}
