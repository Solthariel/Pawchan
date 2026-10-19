package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.update.ReleaseNotesContent
import com.solthariel.pawchan.update.UpdateVersionSummary
import org.junit.Rule
import org.junit.Test

class UpdateNotesRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun availableNotesHaveClearVersionLabelsAndFormattedItems() {
        compose.setContent {
            MaterialTheme {
                Column {
                    UpdateVersionSummary("0.1.4", "0.1.5")
                    ReleaseNotesContent("Версия 0.1.5\n\nИсправления\n\n* Первый пункт\n* Второй пункт")
                }
            }
        }
        compose.onNodeWithTag("update-installed-version").assertIsDisplayed()
        compose.onNodeWithText("Установлена: 0.1.4").assertIsDisplayed()
        compose.onNodeWithTag("update-available-version").assertIsDisplayed()
        compose.onNodeWithText("Доступна: 0.1.5").assertIsDisplayed()
        compose.onAllNodesWithText("Исправления").assertCountEquals(1)
        compose.onAllNodesWithText("•").assertCountEquals(2)
        compose.onNodeWithText("Первый пункт").assertIsDisplayed()
        compose.onNodeWithText("Второй пункт").assertIsDisplayed()
        compose.onAllNodesWithText("Версия 0.1.5").assertCountEquals(0)
        compose.onAllNodesWithText("* Первый пункт").assertCountEquals(0)
        compose.onAllNodesWithText("Изменения").assertCountEquals(0)
    }
}
