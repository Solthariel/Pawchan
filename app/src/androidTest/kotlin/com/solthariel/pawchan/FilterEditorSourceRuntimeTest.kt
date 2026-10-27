package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterEditor
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FilterEditorSourceRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun editingConditionDoesNotOverwriteTheSeededProbeText() {
        val source = "Почему нет ниодного известного беларуса?"
        val rule = newContentFilterRule(defaultBoardAddress(), source)
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        compose.onNodeWithContentDescription("Что искать").performScrollTo().performTextReplacement("беларус")
        compose.onNodeWithText("Часть слова").performScrollTo().performClick()
        val probe = compose.onNodeWithContentDescription("Проверяемый текст").performScrollTo()
        assertEquals(source, probe.fetchSemanticsNode().config[SemanticsProperties.EditableText].text)
    }

    @Test fun literalPunctuationIsValidRatherThanAnEnglishRegexError() {
        val rule = newContentFilterRule(defaultBoardAddress(), "(")
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        compose.onNodeWithContentDescription("Проверяемый текст").performScrollTo().performTextReplacement("(")
        compose.onNodeWithText("Invalid", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Ошибка в регулярном выражении", substring = true).assertDoesNotExist()
    }
}
