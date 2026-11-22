package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import org.junit.Rule
import org.junit.Test

class SwitchAndToolbarRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun draggingTheThumbAndClickingTheRowBothToggleOnce() {
        val checked = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                Row(Modifier.testTag("row").toggleable(checked.value, role = Role.Switch) { checked.value = it }) {
                    CompactSwitch(checked.value, Modifier.testTag("thumb")) { checked.value = it }
                }
            }
        }
        compose.onNodeWithTag("row").assertIsOff()
        compose.onNodeWithTag("thumb", useUnmergedTree = true).performTouchInput { swipeRight() }
        compose.onNodeWithTag("row").assertIsOn()
        compose.onNodeWithTag("thumb", useUnmergedTree = true).performTouchInput { swipeLeft() }
        compose
            .onNodeWithTag("row")
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test fun holdingTheButtonDoesNotShowAPopup() {
        compose.setContent { MaterialTheme { UiToolbarButton(UiIconKind.REFRESH, "Обновить") {} } }
        compose.onNodeWithContentDescription("Обновить").performTouchInput { longClick(Offset(3f, 3f)) }
        compose.onNodeWithText("Обновить").assertDoesNotExist()
    }
}
