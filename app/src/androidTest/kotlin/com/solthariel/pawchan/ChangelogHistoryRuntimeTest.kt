package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import com.solthariel.pawchan.settings.ChangelogSettings
import org.junit.Rule
import org.junit.Test

class ChangelogHistoryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun bundledHistoryShowsInstalledVersionAndCanScrollToFirstRelease() {
        compose.setContent { MaterialTheme { ChangelogSettings() } }
        compose.onNodeWithText("Список изменений").performClick()
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText("Версия ${BuildConfig.VERSION_NAME}").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Версия ${BuildConfig.VERSION_NAME}").assertIsDisplayed()
        compose.onAllNodesWithText("Установлена").assertCountEquals(1)
        compose.onNodeWithTag("changelog-list").performScrollToNode(hasText("Версия 0.1.0"))
        compose.onNodeWithText("Версия 0.1.0").assertIsDisplayed()
        compose.onNodeWithContentDescription("Назад к настройкам").performClick()
        compose.onAllNodesWithText("Установлена").assertCountEquals(0)
    }
}
