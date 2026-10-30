package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
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
    private var fixtureDensity = 1f

    @Test fun compactToolbarKeeps48dpActionsAndMovesSecondaryActionsToMenu() {
        showToolbar(320)
        assertToolbarTargets(listOf("Назад", "Написать", "Поиск", "Обновить", "Меню треда"))
        compose.onNodeWithContentDescription("Галерея").assertDoesNotExist()
        compose.onNodeWithContentDescription("Меню треда").performClick()
        compose.onNodeWithText("Галерея").assertIsDisplayed()
        compose.onNodeWithText("Добавить тред в избранное").assertIsDisplayed()
        saveThreadChromeScreenshot("toolbar-compact")
    }

    @Test fun wideToolbarKeepsDirectBookmarkAndGalleryActions() {
        showToolbar(420)
        assertToolbarTargets(listOf("Назад", "Добавить тред в избранное", "Галерея", "Написать", "Поиск", "Обновить", "Меню треда"))
    }

    private fun showToolbar(width: Int) {
        compose.setContent {
            val scope = rememberCoroutineScope()
            val tools = remember { ThreadToolsState(scope) }
            val density = LocalDensity.current
            fixtureDensity = density.density * 0.75f
            CompositionLocalProvider(LocalDensity provides Density(fixtureDensity, density.fontScale)) {
                ThreadTestTheme(ThemePreference.DARK) {
                    Box(Modifier.width(width.dp).statusBarsPadding().testTag("toolbar-fixture")) {
                        ThreadToolbar(testContent(), ThreadActions({}, {}, {}, {}), tools) {}
                    }
                }
            }
        }
    }

    private fun assertToolbarTargets(descriptions: List<String>) {
        val toolbar = compose.onNodeWithTag("toolbar-fixture").fetchSemanticsNode().boundsInRoot
        val slot = kotlin.math.round(48f * fixtureDensity)
        val bounds =
            descriptions.map { description ->
                compose.onNodeWithContentDescription(description).fetchSemanticsNode().boundsInRoot
            }
        bounds.forEach {
            assertEquals(slot.toFloat(), it.width, 1f)
            assertEquals(slot.toFloat(), it.height, 1f)
            org.junit.Assert.assertTrue(it.left >= toolbar.left && it.right <= toolbar.right)
        }
        bounds.zipWithNext().forEach { (left, right) -> org.junit.Assert.assertTrue(left.right <= right.left + 1f) }
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
