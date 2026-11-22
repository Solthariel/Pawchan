package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class UiConnectedChoicesRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun choicesExposeSelectionAndEditableAction() {
        val selected = mutableIntStateOf(0)
        val enabled = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    UiConnectedChoices(listOf("75%", "85%", "Другое"), selected.intValue, enabled.value, "Ввести") {
                        selected.intValue = it
                    }
                }
            }
        }
        compose.onNodeWithText("75%").assertIsSelected()
        compose.onNodeWithText("85%").performClick().assertIsSelected()
        compose.onNodeWithContentDescription("Ввести").performClick().assertIsSelected()
        compose.runOnIdle {
            assertEquals(2, selected.intValue)
            enabled.value = false
        }
        compose.onNodeWithContentDescription("Ввести").assertIsNotEnabled()
    }
}
