package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterEditor
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Rule
import org.junit.Test

class ContentFilterEditorWindowRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun saveRemainsEnabledImmediatelyAfterEachValidEdit() {
        val rule = newContentFilterRule(defaultBoardAddress(), "кот")
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, "typing", {}, {}) } }
        listOf("ко", "кот", "котик", "новая фраза").forEach { value ->
            compose.onNodeWithContentDescription("Что искать").performTextReplacement(value)
            compose.onNodeWithText("Сохранить").assertIsEnabled()
        }
    }
}
