package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.settings.MirrorAvailabilityPresentation
import com.solthariel.pawchan.settings.MirrorAvailabilityResults
import com.solthariel.pawchan.settings.MirrorStatusRow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MirrorAvailabilityResultsRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val result =
        MirrorAvailabilityPresentation(
            rows = listOf(MirrorStatusRow("2ch.su", "Доступно", "HTTP-проверка: 123 мс")),
            summary = "Доступны 1 из 1 — проверено в 12:00:00",
        )

    @Test fun expandingAndCollapsingDoesNotRequestNetworkOrDiscardResults() {
        var requests = 0
        compose.setContent { MaterialTheme { Column { MirrorAvailabilityResults(result, false) { requests++ } } } }
        compose.onNodeWithText("2ch.su").assertDoesNotExist()
        compose.onNodeWithText("Доступность зеркал").performClick()
        compose.onNodeWithText("Доступно — HTTP-проверка: 123 мс").assertIsDisplayed()
        compose.onNodeWithText("Доступность зеркал").performClick()
        compose.onNodeWithText("2ch.su").assertDoesNotExist()
        compose.onNodeWithText(requireNotNull(result.summary)).assertIsDisplayed()
        compose.onNodeWithText("Доступность зеркал").performClick()
        compose.onNodeWithText("2ch.su").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, requests) }
    }

    @Test fun refreshOpensResultsAndStaysOpenWhenCheckCompletes() {
        var checking by mutableStateOf(false)
        var presentation by mutableStateOf(MirrorAvailabilityPresentation())
        var requests = 0
        compose.setContent {
            MaterialTheme {
                Column {
                    MirrorAvailabilityResults(presentation, checking) {
                        requests++
                        checking = true
                    }
                }
            }
        }
        compose.onNodeWithContentDescription("Проверить доступность").performClick().assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, requests)
            presentation = result
            checking = false
        }
        compose.onNodeWithText("2ch.su").assertIsDisplayed()
        compose.onNodeWithText("Доступность зеркал").performClick()
        compose.onNodeWithText("2ch.su").assertDoesNotExist()
        compose.onNodeWithContentDescription("Проверить доступность").performClick()
        compose.runOnIdle { assertEquals(2, requests) }
    }
}
