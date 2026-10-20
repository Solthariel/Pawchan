package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterDraft
import com.solthariel.pawchan.settings.ContentFilterProbe
import com.solthariel.pawchan.settings.FilterEditorBody
import com.solthariel.pawchan.settings.FilterEditorFields
import com.solthariel.pawchan.settings.FilterEditorViewport
import com.solthariel.pawchan.settings.FilterLineInput
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentFilterTopWindowRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changingKeyboardInsetsNeverRecentersTheCompactWindow() {
        val draft = ContentFilterDraft(newContentFilterRule(defaultBoardAddress(), "кот"))
        var keyboard by mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme {
                val inset = with(LocalDensity.current) { keyboard.dp.roundToPx() }
                Box(Modifier.width(360.dp).height(620.dp)) {
                    FilterEditorViewport({}, bottomInsets = WindowInsets(0, 0, 0, inset)) {
                        FilterEditorBody(draft, {}, null, { TextButton(onClick = {}) { Text("Сохранить") } }) {
                            FilterLineInput("Условие", draft.text, { draft.text = it })
                        }
                    }
                }
            }
        }
        val window = compose.onNodeWithTag("filter-editor-window")
        val top = window.fetchSemanticsNode().boundsInRoot.top
        listOf(80, 160, 240, 160, 0).forEach { inset ->
            compose.runOnIdle { keyboard = inset }
            assertEquals(top, window.fetchSemanticsNode().boundsInRoot.top, 1f)
            compose.onNodeWithText("Сохранить").assertIsDisplayed()
        }
    }

    @Test fun aShortKeyboardViewportKeepsActionsVisibleAndTheProbeReachable() {
        val rule = newContentFilterRule(defaultBoardAddress(), "кот")
        val draft = ContentFilterDraft(rule)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp).height(340.dp)) {
                    FilterEditorViewport({}, bottomInsets = WindowInsets(0, 0, 0, 0)) {
                        FilterEditorBody(draft, {}, null, { TextButton(onClick = {}) { Text("Сохранить") } }) {
                            FilterEditorFields(draft)
                            ContentFilterProbe(rule)
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
        compose.onNodeWithTag("filter-editor-enabled").assertIsDisplayed()
        compose.onNodeWithContentDescription("Проверяемый текст").performScrollTo().performTextReplacement("кот")
        compose.onNodeWithContentDescription("Проверяемый текст").assertIsDisplayed()
        compose.onNodeWithText("Сохранить").assertIsDisplayed()
    }

    @Test fun regularWidthPutsToggleAndActionsInOneRowWithoutBreakingTheirCallbacks() {
        val draft = ContentFilterDraft(newContentFilterRule(defaultBoardAddress(), "кот"))
        var saved = false
        var closed = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(380.dp).height(620.dp)) {
                    FilterEditorBody(draft, { closed = true }, null, {
                        TextButton(onClick = { saved = true }) { Text("Сохранить") }
                    }) {
                        FilterLineInput("Условие", draft.text, { draft.text = it })
                    }
                }
            }
        }
        val toggle = compose.onNodeWithTag("filter-editor-enabled")
        val save = compose.onNodeWithText("Сохранить")
        assertEquals(
            toggle
                .fetchSemanticsNode()
                .boundsInRoot.center.y,
            save
                .fetchSemanticsNode()
                .boundsInRoot.center.y,
            1f,
        )
        toggle.performClick()
        compose.runOnIdle { assertTrue(!draft.enabled) }
        save.performClick()
        compose.onNodeWithText("Отмена").performClick()
        compose.runOnIdle { assertTrue(saved && closed) }
    }
}
