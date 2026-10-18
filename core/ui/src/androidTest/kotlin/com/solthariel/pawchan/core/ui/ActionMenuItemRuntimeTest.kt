package com.solthariel.pawchan.core.ui

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ActionMenuItemRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun enabledItemInvokesTheSuppliedActionOnce() {
        var calls = 0
        compose.setContent { ActionMenuItem("Действие", { calls++ }) }
        compose.onNodeWithText("Действие").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun disabledItemKeepsTheLabelAndCannotInvokeTheAction() {
        var calls = 0
        compose.setContent { ActionMenuItem("Недоступно", { calls++ }, enabled = false) }
        compose.onNodeWithText("Недоступно").assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(0, calls) }
    }
}
