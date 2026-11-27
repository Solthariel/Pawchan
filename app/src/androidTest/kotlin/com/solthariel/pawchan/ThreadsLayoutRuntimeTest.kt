package com.solthariel.pawchan

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.domain.settings.NavigationLayout
import com.solthariel.pawchan.domain.settings.NavigationPreferences
import com.solthariel.pawchan.domain.settings.ThreadsHubItem
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.threads.ThreadsDisplayDialog
import com.solthariel.pawchan.threads.ThreadsLinkActions
import com.solthariel.pawchan.threads.ThreadsPageActions
import com.solthariel.pawchan.threads.ThreadsScreen
import com.solthariel.pawchan.threads.ThreadsScreenActions
import com.solthariel.pawchan.threads.ThreadsScreenInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** «⋮ → Отображение списка»: порядок разделов и ссылки в ⋮; по умолчанию экран прежний. */
class ThreadsLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private var historyOpened = 0

    @Test
    fun defaultKeepsLinksBelowTheSections() {
        show(NavigationLayout())
        assertTrue(top("История") > top("Ответы"))
    }

    @Test
    fun movedLinkIsShownAboveTheSections() {
        show(NavigationLayout(threads = listOf(ThreadsHubItem.HISTORY)))
        assertTrue(top("История") < top("Страницы"))
    }

    @Test
    fun linkInTheMenuLeavesTheScreenAndOpensFromTheMenu() {
        show(NavigationLayout(threadsInMenu = setOf(ThreadsHubItem.HISTORY)))
        compose.onNodeWithText("История").assertDoesNotExist()
        compose.onNodeWithContentDescription("Меню").performClick()
        compose.onNodeWithText("История").performClick()
        compose.runOnIdle { assertEquals(1, historyOpened) }
    }

    @Test
    fun dialogMovesHidesAndResets() {
        val value = mutableStateOf(NavigationPreferences())
        compose.setContent { MaterialTheme { ThreadsDisplayDialog(value.value, { value.value = it }, {}) } }
        compose.onNodeWithContentDescription("Выше: История").performClick()
        compose.onNodeWithContentDescription("Показывать на экране: Загрузки").performClick()
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle {
            assertEquals(
                listOf(
                    ThreadsHubItem.PAGES,
                    ThreadsHubItem.FAVORITES,
                    ThreadsHubItem.HISTORY,
                    ThreadsHubItem.REPLIES,
                    ThreadsHubItem.COPIES,
                    ThreadsHubItem.DOWNLOADS,
                ),
                value.value.layout.threads,
            )
            assertEquals(setOf(ThreadsHubItem.DOWNLOADS), value.value.layout.threadsInMenu)
        }
        compose.onNodeWithText("Сбросить").performClick()
        compose.onNodeWithText("Применить").performClick()
        compose.runOnIdle { assertEquals(NavigationPreferences(), value.value) }
    }

    private fun show(layout: NavigationLayout) {
        InstrumentationRegistry
            .getInstrumentation()
            .targetContext
            .getSharedPreferences("threads_hub", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        compose.setContent {
            MaterialTheme {
                ThreadsScreen(
                    ThreadsScreenInput(emptyList(), emptyList(), emptyList(), null, emptySet(), 0, false),
                    ThreadsScreenActions(
                        open = { _, _ -> },
                        pages = ThreadsPageActions({}, {}),
                        setFavorite = { _, _ -> },
                        dismissReplies = {},
                        copyLink = {},
                        refresh = {},
                        links = ThreadsLinkActions({ historyOpened++ }, {}),
                    ),
                    AppBottomActions({}, {}, {}, {}),
                    onBack = {},
                    limits = NavigationPreferences(layout = layout),
                )
            }
        }
        compose.waitForIdle()
    }

    private fun top(text: String): Float =
        compose
            .onNodeWithText(text)
            .fetchSemanticsNode()
            .boundsInRoot.top
}
