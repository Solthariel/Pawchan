package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterEngine
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterDraft
import com.solthariel.pawchan.settings.ContentFilterEditor
import com.solthariel.pawchan.settings.ContentFilterResetNotice
import com.solthariel.pawchan.settings.FilterPostScope
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class FilterGroupsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun androidEngineMatchesOneAlternativePerRequiredRowAndRespectsScope() {
        val board = defaultBoardAddress()
        val rule =
            newContentFilterRule(board).copy(
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_TERMS, listOf("пустите", "фап, fap")),
                targets = ContentFilterTargets(comments = false),
            )
        val engine = ContentFilterEngine(listOf(rule))
        listOf("пустите в фап", "пустите меня в Fap", "в /fap/ меня пустите").forEach {
            assertNotNull(engine.thread(board, it, ""))
            assertNull(engine.comment(board, it))
        }
        assertNull(engine.thread(board, "пустите домой", ""))
        assertNull(engine.thread(board, "просто fap", ""))
    }

    @Test fun editorSavesCommaGroupsAndChosenPostKindWithoutRegexOrCaseControls() {
        val base = newContentFilterRule(defaultBoardAddress(), "")
        val saved = AtomicReference<ContentFilterRule?>()
        compose.setContent { MaterialTheme { ContentFilterEditor(base, base.id, {}, { saved.set(it) }) } }
        compose
            .onNodeWithText("ОП-посты")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose
            .onNodeWithText("Все условия")
            .performScrollTo()
            .performClick()
            .assertIsSelected()
        compose.onNodeWithContentDescription("Что искать").performScrollTo().performTextReplacement("пустите\nфап, fap")
        compose.waitUntil(10_000) {
            compose
                .onNodeWithText(
                    "Сохранить",
                ).fetchSemanticsNode()
                .config
                .contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)
                .not()
        }
        compose.onNodeWithText("Сохранить").performClick()
        compose.waitUntil(10_000) { saved.get() != null }
        compose.runOnIdle {
            val result = checkNotNull(saved.get())
            assertEquals(listOf("пустите", "фап, fap"), result.values)
            assertEquals(ContentFilterMode.ALL_TERMS, result.mode)
            assertTrue(result.threads && !result.comments)
            assertEquals(FilterPostScope.OP.name, ContentFilterDraft(result).postScope)
        }
        compose.onNodeWithText("Учитывать регистр").assertDoesNotExist()
        compose.onNodeWithText("Регулярное выражение").assertDoesNotExist()
    }

    @Test fun resetNoticeHasAnExplicitDismissAction() {
        var dismissed = false
        compose.setContent { MaterialTheme { ContentFilterResetNotice { dismissed = true } } }
        compose.onNodeWithText("Сохранённые правила автоскрытия сброшены. Создайте их заново.").assertExists()
        compose.onNodeWithText("Понятно").performClick()
        compose.runOnIdle { assertTrue(dismissed) }
    }

    @Test fun bundledHelpDescribesCurrentGroupsAndPostKindsWithoutVersionHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val text =
            context.assets
                .open("AUTOHIDE.md")
                .bufferedReader()
                .use { it.readText() }
        listOf("Любой вариант", "Все условия", "ОП-посты", "Комментарии", "фап, fap").forEach {
            assertTrue(text.contains(it))
        }
        assertTrue(text.contains("без учёта регистра"))
        assertFalse(text.contains("Новый формат правил"))
        assertFalse(text.contains("Правила предыдущей версии"))
    }
}
