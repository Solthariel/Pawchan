package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.settings.ContentFilterEditor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class ContentFilterModeRoundTripRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun reopenedWordsBecomeExactPhraseWithoutGeneratedPunctuation() {
        val rule =
            ContentFilterRule(
                "words",
                "2ch",
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_TERMS, listOf("голова", "дай", "денег")),
            )
        val saved = AtomicReference<ContentFilterRule?>()
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, { saved.set(it) }) } }
        compose.onNodeWithText("Фраза").performScrollTo().performClick()
        assertEquals(
            "голова\nдай\nденег",
            compose
                .onNodeWithContentDescription("Что искать")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        compose.waitUntil(timeoutMillis = 5_000) {
            !compose
                .onNodeWithText("Сохранить")
                .fetchSemanticsNode()
                .config
                .contains(SemanticsProperties.Disabled)
        }
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(timeoutMillis = 5_000) { saved.get() != null }
        compose.runOnIdle {
            assertEquals(ContentFilterMode.PHRASE, saved.get()?.mode)
            assertEquals(listOf("голова\nдай\nденег"), saved.get()?.values)
        }
    }

    @Test
    fun exactPhraseKeepsUserPunctuationAcrossRadioButtonSwitches() {
        val text = "хлеб, молоко!"
        val rule =
            ContentFilterRule(
                "phrase",
                "2ch",
                condition = ContentFilterConditionSpec(ContentFilterMode.PHRASE, listOf(text)),
            )
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        compose.onNodeWithText("Любое из слов").performScrollTo().performClick()
        compose.onNodeWithText("Фраза").performScrollTo().performClick()
        assertEquals(
            text,
            compose
                .onNodeWithContentDescription("Что искать")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
    }
}
