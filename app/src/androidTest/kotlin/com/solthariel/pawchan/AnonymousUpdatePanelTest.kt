package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.settings.releaseUpdateConfiguration
import com.solthariel.pawchan.update.UpdatePanel
import com.solthariel.pawchan.update.rememberUpdatePanel
import org.junit.Rule
import org.junit.Test

class AnonymousUpdatePanelTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ordinaryUpdatePanelNeverOffersTokenEntry() {
        compose.setContent {
            MaterialTheme {
                // An empty test feed keeps the scenario deterministic and makes no network request.
                UpdatePanel(rememberUpdatePanel(releaseUpdateConfiguration().copy(manifestUrl = "")))
            }
        }
        compose.onNodeWithText("Обновления").assertIsDisplayed()
        compose.onNodeWithText("Закрытые обновления").assertDoesNotExist()
        compose.onNodeWithText("GitHub token — Contents: Read").assertDoesNotExist()
        compose.onNodeWithText("Сохранить доступ").assertDoesNotExist()
        compose.onNodeWithText("Проверить обновления").performClick()
        compose.onNodeWithText("GitHub token — Contents: Read").assertDoesNotExist()
        compose.onNodeWithText("Проверить обновления").assertIsDisplayed()
    }

    @Test fun aboutUpdatePanelKeepsCheckActionWithoutRepeatingVersion() {
        compose.setContent {
            MaterialTheme {
                UpdatePanel(
                    rememberUpdatePanel(releaseUpdateConfiguration().copy(manifestUrl = "")),
                    showInstalledVersion = false,
                )
            }
        }
        compose.onNodeWithText("Установлена: ${BuildConfig.VERSION_NAME}").assertDoesNotExist()
        compose.onNodeWithText("Обновления").assertIsDisplayed()
        compose.onNodeWithText("Проверить обновления").performClick()
        compose.onNodeWithText("Проверить обновления").assertIsDisplayed()
        compose.onNodeWithText("Установлена: ${BuildConfig.VERSION_NAME}").assertDoesNotExist()
    }
}
