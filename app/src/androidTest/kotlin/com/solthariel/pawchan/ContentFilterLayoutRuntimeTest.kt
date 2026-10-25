package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterDraft
import com.solthariel.pawchan.settings.ContentFilterProbe
import com.solthariel.pawchan.settings.FilterEditorFields
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContentFilterLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchModesShareOneRowAndNoEmptyConditionErrorIsShown() {
        val draft = ContentFilterDraft(newContentFilterRule(defaultBoardAddress()))
        compose.setContent { MaterialTheme { Column(Modifier.width(320.dp)) { FilterEditorFields(draft) } } }
        val phrase = compose.onNodeWithText("Точная фраза").fetchSemanticsNode().boundsInRoot
        val words = compose.onNodeWithText("Все слова").fetchSemanticsNode().boundsInRoot
        assertEquals(phrase.top, words.top, 1f)
        compose.onNodeWithText("Условие пусто").assertDoesNotExist()
        val condition = compose.onNodeWithContentDescription("Условие")
        condition.performClick()
        compose.onNodeWithText("Введите фразу целиком").assertExists()
        compose.onNodeWithText("Все слова").performClick()
        condition.performClick()
        compose.onNodeWithText("Слова через пробел или запятую. Порядок не важен").assertExists()
        compose.onNodeWithText("Условие пусто").assertDoesNotExist()
    }

    @Test fun probeHeightDoesNotCollapseBetweenRuleChanges() {
        var rule by mutableStateOf(newContentFilterRule(defaultBoardAddress(), "кот"))
        compose.setContent {
            MaterialTheme { Column(Modifier.width(320.dp).testTag("probe")) { ContentFilterProbe(rule) } }
        }
        val initial =
            compose
                .onNodeWithTag("probe")
                .fetchSemanticsNode()
                .boundsInRoot.height
        compose.onNodeWithContentDescription("Проверяемый текст").performTextReplacement("кот")
        compose.waitForIdle()
        assertEquals(
            initial,
            compose
                .onNodeWithTag("probe")
                .fetchSemanticsNode()
                .boundsInRoot.height,
            1f,
        )
        compose.runOnIdle { rule = newContentFilterRule(defaultBoardAddress(), "собака") }
        compose.waitForIdle()
        assertEquals(
            initial,
            compose
                .onNodeWithTag("probe")
                .fetchSemanticsNode()
                .boundsInRoot.height,
            1f,
        )
    }
}
