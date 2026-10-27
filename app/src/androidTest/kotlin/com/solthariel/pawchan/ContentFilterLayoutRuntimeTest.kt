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
import androidx.compose.ui.test.performScrollTo
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

    @Test fun builderOffersOnlyThreeModesAndNoRemovedTargets() {
        val rule = newContentFilterRule(defaultBoardAddress(), "кот")
        compose.setContent {
            MaterialTheme {
                com.solthariel.pawchan.settings
                    .ContentFilterEditor(rule, rule.id, {}, {})
            }
        }
        listOf("Фраза", "Любое слово", "Все слова").forEach { label ->
            compose.onNodeWithText(label).performScrollTo().assertExists()
        }
        compose.onNodeWithText("Слово целиком").performScrollTo().assertExists()
        compose.onNodeWithText("Часть слова").performScrollTo().assertExists()
        compose.onNodeWithText("Начало слова").assertDoesNotExist()
        compose.onNodeWithText("Учитывать регистр").assertDoesNotExist()
        compose.onNodeWithText("ОП-посты").assertDoesNotExist()
        compose.onNodeWithText("Ответы").assertDoesNotExist()
        compose.onNodeWithText("Регулярное выражение").assertDoesNotExist()
        compose.onNodeWithContentDescription("Копировать").assertDoesNotExist()
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
