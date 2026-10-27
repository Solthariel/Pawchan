package com.solthariel.pawchan

import android.content.ClipboardManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterEditor
import com.solthariel.pawchan.settings.filterRegexPrompt
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FilterRegexCopyRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun copyingIncludesTheWholeConditionWithoutEditingItAndShowsTheNotice() {
        val text = "Аниме\nманга \\d+"
        val rule = newContentFilterRule(defaultBoardAddress(), text)
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        compose.onNodeWithText("Регулярное выражение").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Условие").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Копировать").performScrollTo().performClick()
        compose.onNodeWithTag("filter-copy-notice").assertIsDisplayed()
        assertEquals(
            text,
            compose
                .onNodeWithContentDescription("Условие")
                .fetchSemanticsNode()
                .config[SemanticsProperties.EditableText]
                .text,
        )
        compose.runOnIdle {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val clipboard = requireNotNull(context.getSystemService(ClipboardManager::class.java))
            assertEquals(
                filterRegexPrompt(text),
                clipboard.primaryClip
                    ?.getItemAt(0)
                    ?.text
                    ?.toString(),
            )
        }
    }

    @Test fun simpleModeDoesNotOfferTheAiCopyButton() {
        val rule = newContentFilterRule(defaultBoardAddress(), "обычная фраза")
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        compose.onNodeWithContentDescription("Копировать").assertDoesNotExist()
    }
}
