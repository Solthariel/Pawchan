package com.solthariel.pawchan.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiRefreshButtonUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun usesCallerDescriptionAndClickAction() {
        var calls = 0
        compose.setContent { MaterialTheme { UiRefreshButton(false, { calls++ }, "Reload forum") } }
        compose.onNodeWithContentDescription("Reload forum").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun callerCanDisableRefreshWhileAnUpdateIsRunning() {
        compose.setContent { MaterialTheme { UiRefreshButton(true, {}, "Обновить", enabled = false) } }
        compose.onNodeWithContentDescription("Обновить").assertIsNotEnabled()
    }

    @Test fun completedRefreshEnablesTheButtonAndPreservesItsClick() {
        val refreshing = mutableStateOf(true)
        var calls = 0
        compose.setContent {
            MaterialTheme { UiRefreshButton(refreshing.value, { calls++ }, "Обновить", enabled = !refreshing.value) }
        }
        compose.onNodeWithContentDescription("Обновить").assertIsNotEnabled()
        compose.runOnIdle { refreshing.value = false }
        compose.onNodeWithContentDescription("Обновить").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }
}
