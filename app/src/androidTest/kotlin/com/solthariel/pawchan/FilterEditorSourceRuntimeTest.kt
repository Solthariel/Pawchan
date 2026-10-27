package com.solthariel.pawchan

import android.content.ClipboardManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterEditor
import com.solthariel.pawchan.settings.filterRegexPrompt
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FilterEditorSourceRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun sourceIsSeededAndCopyDoesNotCopyTheEditedExpression() {
        val source = "Почему нет ниодного известного беларуса?"
        val rule = newContentFilterRule(defaultBoardAddress(), source)
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        val probe = compose.onNodeWithContentDescription("Проверяемый текст")
        assertEquals(source, probe.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
        compose.onNodeWithText("Регулярное выражение").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Условие").performTextReplacement("беларус")
        compose.onNodeWithContentDescription("Копировать").performScrollTo().performClick()
        compose.runOnIdle {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val clipboard = requireNotNull(context.getSystemService(ClipboardManager::class.java))
            assertEquals(
                filterRegexPrompt(source),
                clipboard.primaryClip
                    ?.getItemAt(0)
                    ?.text
                    ?.toString(),
            )
        }
    }

    @Test fun invalidExpressionShowsLocalizedErrorNotNoMatches() {
        val rule = newContentFilterRule(defaultBoardAddress(), "кот")
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        compose.onNodeWithText("Регулярное выражение").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Условие").performTextReplacement("(")
        compose.waitUntil(5_000) {
            compose.onAllNodesWithText("Ошибка в регулярном выражении", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Нет совпадений").assertDoesNotExist()
        compose.onNodeWithText("Invalid", substring = true).assertDoesNotExist()
    }
}
