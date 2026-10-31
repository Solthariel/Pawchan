package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.thread.PostRepliesFooter
import com.solthariel.pawchan.thread.ThreadActions
import com.solthariel.pawchan.thread.ThreadContentFlags
import com.solthariel.pawchan.thread.ThreadContentPresentation
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadSelectionActions
import com.solthariel.pawchan.thread.ThreadToolbar
import com.solthariel.pawchan.thread.ThreadToolsState
import com.solthariel.pawchan.thread.ThreadTopBar
import com.solthariel.pawchan.thread.ThreadUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThreadChromeUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun normalToolbarUsesCatalogueEdgeSlots() {
        compose.setContent {
            val scope = rememberCoroutineScope()
            val tools = remember { ThreadToolsState(scope) }
            ThreadTestTheme(ThemePreference.DARK) {
                Box(Modifier.width(360.dp).statusBarsPadding().testTag("toolbar-fixture")) {
                    ThreadToolbar(testContent(), ThreadActions({}, {}, {}, {}), tools) {}
                }
            }
        }
        val descriptions = listOf("Назад", "Добавить тред в избранное", "Галерея", "Написать", "Поиск", "Обновить", "Меню треда")
        val toolbar = compose.onNodeWithTag("toolbar-fixture").fetchSemanticsNode().boundsInRoot
        val inset = with(compose.density) { 12.dp.roundToPx() }
        val slot = with(compose.density) { 36.dp.roundToPx() }
        // Compose rounds each measured slot, not the sum of dp coordinates.
        val expectedCenters =
            List(3) { toolbar.left + inset + slot * (it + 0.5f) } +
                List(4) { toolbar.right - inset - slot * (3.5f - it) }
        descriptions.forEachIndexed { index, description ->
            val bounds = compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
            assertEquals(slot.toFloat(), bounds.width, 0f)
            assertEquals(expectedCenters[index], bounds.center.x, 1f)
        }
        saveThreadChromeScreenshot("toolbar")
    }

    @Test
    fun selectionReplacesTheNormalToolbarWithIconsAndRestoresItOnCancel() {
        var screenshots = 0
        var quotes = 0
        var reports = 0
        compose.setContent {
            val scope = rememberCoroutineScope()
            val tools =
                remember {
                    ThreadToolsState(scope).apply {
                        toggleSelection(1)
                        toggleSelection(2)
                    }
                }
            ThreadTestTheme(ThemePreference.DARK) {
                Box(Modifier.width(360.dp).statusBarsPadding().testTag("toolbar-fixture")) {
                    ThreadTopBar(
                        testContent(),
                        ThreadActions({}, {}, {}, {}),
                        tools,
                        ThreadSelectionActions({ quotes++ }, { screenshots++ }, { reports++ }),
                    ) {}
                }
            }
        }
        compose.onNodeWithText("Выбрано: 2").assertIsDisplayed()
        compose.onNodeWithContentDescription("Поиск").assertDoesNotExist()
        compose.onNodeWithText("Цитировать").assertDoesNotExist()
        compose.onNodeWithText("Скриншот").assertDoesNotExist()
        compose.onNodeWithText("Жалоба").assertDoesNotExist()
        compose.onNodeWithContentDescription("Скриншот").performClick()
        compose.onNodeWithContentDescription("Цитировать").performClick()
        compose.onNodeWithContentDescription("Жалоба").performClick()
        assertEquals(1, screenshots)
        assertEquals(1, quotes)
        assertEquals(1, reports)
        saveThreadChromeScreenshot("selection")
        compose.onNodeWithContentDescription("Отменить выделение").performClick()
        compose.onNodeWithText("Выбрано: 2").assertDoesNotExist()
        compose.onNodeWithContentDescription("Поиск").assertIsDisplayed()
    }

    @Test
    fun wholeReplyFooterIsClickableIncludingItsRightEdge() {
        var clicks = 0
        compose.setContent {
            ThreadTestTheme(ThemePreference.DARK) {
                Box(
                    Modifier.width(360.dp).statusBarsPadding(),
                ) { PostRepliesFooter("Ответов: 36", true, onClick = { clicks++ }) }
            }
        }
        compose.onNodeWithText("Ответов: 36").performTouchInput {
            click(Offset(width - 2f, height / 2f))
        }
        assertEquals(1, clicks)
    }

    private fun testContent(): ThreadUiState.Content =
        ThreadUiState.Content(
            1,
            null,
            emptyList(),
            ThreadContentFlags(false),
            ThreadContentPresentation(false),
            ThreadContentStatistics(),
        )
}
