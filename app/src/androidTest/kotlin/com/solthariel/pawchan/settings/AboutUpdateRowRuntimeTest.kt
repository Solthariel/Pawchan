package com.solthariel.pawchan.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.update.rememberUpdatePanel
import org.junit.Rule
import org.junit.Test

class AboutUpdateRowRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun versionRowStartsCheckOnlyAfterUserClick() {
        compose.setContent {
            MaterialTheme {
                AboutUpdateRow(rememberUpdatePanel(releaseUpdateConfiguration().copy(manifestUrl = "")))
            }
        }
        compose.onNodeWithText("Проверка обновлений").assertDoesNotExist()
        compose.onNodeWithText("Проверить обновления").performClick()
        compose.onNodeWithText("Проверка обновлений").assertDoesNotExist()
        compose.onNodeWithText("Проверить обновления").assertIsDisplayed()
    }
}
