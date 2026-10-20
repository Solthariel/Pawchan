package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterDraft
import com.solthariel.pawchan.settings.ContentFilterEditor
import com.solthariel.pawchan.settings.ContentFilterProbe
import com.solthariel.pawchan.settings.FilterEditorBody
import com.solthariel.pawchan.settings.FilterEditorFields
import com.solthariel.pawchan.settings.FilterLegacyFields
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Rule
import org.junit.Test

class ContentFilterEditorWindowRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortViewportKeepsActionsVisibleAndProbeReachableByScrolling() {
        val rule = newContentFilterRule(defaultBoardAddress(), "кот")
        val draft = ContentFilterDraft(rule)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp).height(340.dp)) {
                    FilterEditorBody(draft, {}, null, { TextButton(onClick = {}) { Text("Сохранить") } }) {
                        FilterEditorFields(draft)
                        ContentFilterProbe(rule)
                        FilterLegacyFields(draft)
                    }
                }
            }
        }
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
        compose.onNodeWithTag("filter-editor-enabled").assertIsDisplayed()
        compose.onNodeWithContentDescription("Проверяемый текст").performScrollTo().performTextReplacement("кот")
        compose.onNodeWithContentDescription("Проверяемый текст").assertIsDisplayed()
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
        compose.onNodeWithTag("filter-editor-enabled").assertIsDisplayed()
    }

    @Test fun saveRemainsEnabledImmediatelyAfterEachValidEdit() {
        val rule = newContentFilterRule(defaultBoardAddress(), "кот")
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, "typing", {}, {}) } }
        listOf("ко", "кот", "котик", "новая фраза").forEach { value ->
            compose.onNodeWithContentDescription("Условие").performTextReplacement(value)
            compose.onNodeWithText("Сохранить").assertIsEnabled()
        }
    }
}
