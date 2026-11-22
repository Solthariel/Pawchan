package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
        )

    @Test fun checkingOpensASeparateWindowAndClosingHidesAllResults() {
        var requests = 0
        compose.setContent { MaterialTheme { Column { MirrorAvailabilityResults(result, false) { requests++ } } } }
        compose.onNodeWithText("2ch.su").assertDoesNotExist()
        compose.onNodeWithText("Проверить зеркала").performClick()
        compose.onNodeWithText("Проверка зеркал").assertIsDisplayed()
        compose.onNodeWithText("2ch.su").assertIsDisplayed()
        compose.onNode(hasText("Доступно", substring = true) and hasText("123 мс", substring = true)).assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.onNodeWithText("Закрыть").performClick()
        compose.onNodeWithText("2ch.su").assertDoesNotExist()
        compose.onNodeWithText("Проверка зеркал").assertDoesNotExist()
        compose.onNodeWithText("Доступность зеркал").assertDoesNotExist()
    }

    @Test fun repeatIsDisabledWhileCheckingAndCompletingDoesNotRequestAgain() {
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
        compose.onNodeWithText("Проверить зеркала").performClick()
        compose.onNodeWithText("Повторить").assertIsNotEnabled()
        compose.onNodeWithText("Проверяем доступность…").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, requests)
            presentation = result
            checking = false
        }
        compose.onNodeWithText("2ch.su").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.onNodeWithText("Повторить").performClick().assertIsNotEnabled()
        compose.runOnIdle { assertEquals(2, requests) }
        compose.onNodeWithText("Закрыть").performClick()
        compose.onNodeWithText("Проверка зеркал").assertDoesNotExist()
    }

    @Test fun reopeningDuringAnExistingCheckDoesNotStartAnotherRequest() {
        var requests = 0
        compose.setContent { MaterialTheme { MirrorAvailabilityResults(result, true) { requests++ } } }
        compose.onNodeWithText("Проверить зеркала").performClick()
        compose.onNodeWithText("Повторить").assertIsNotEnabled()
        compose.onNodeWithText("Закрыть").performClick()
        compose.onNodeWithText("Проверить зеркала").performClick()
        compose.runOnIdle { assertEquals(0, requests) }
    }
}
