package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.AutohideHelpDialog
import com.solthariel.pawchan.settings.ContentFilterEditor
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean

class FilterRegexCopyRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noModeOffersAiCopyOrRegexControls() {
        val rule = newContentFilterRule(defaultBoardAddress(), "обычная фраза")
        compose.setContent { MaterialTheme { ContentFilterEditor(rule, rule.id, {}, {}) } }
        listOf("Фраза", "Любое слово", "Все слова").forEach {
            compose.onNodeWithText(it).performScrollTo().performClick()
            compose.onNodeWithContentDescription("Копировать").assertDoesNotExist()
            compose.onNodeWithText("Регулярное выражение").assertDoesNotExist()
        }
    }

    @Test fun helpIsBundledAndCanBeReadWithoutARequest() {
        val closed = AtomicBoolean(false)
        compose.setContent { MaterialTheme { AutohideHelpDialog { closed.set(true) } } }
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasText("## Что искать", substring = true)).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onAllNodes(hasScrollAction()).assertCountEquals(1)
        val scroll = compose.onNode(hasScrollAction())
        val initial = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue(initial.maxValue().isFinite())
        assertTrue(initial.maxValue() > 0f)
        scroll.performSemanticsAction(SemanticsActions.ScrollBy) { action ->
            assertTrue(action(0f, initial.maxValue()))
        }
        compose.waitForIdle()
        val end = scroll.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange]
        assertEquals(end.maxValue(), end.value(), 1f)
        compose.onNodeWithText("Открыть на GitHub").assertIsDisplayed()
        compose.onNodeWithText("Закрыть").assertIsDisplayed().performClick()
        compose.runOnIdle { assertTrue(closed.get()) }
    }
}
