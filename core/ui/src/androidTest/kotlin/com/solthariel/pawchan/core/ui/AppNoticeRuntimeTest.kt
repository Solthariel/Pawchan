package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppNoticeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun noticeNeverMovesContentAndRepeatedEventReappears() {
        val message = mutableStateOf<String?>(null)
        val event = mutableStateOf(0)
        compose.setContent {
            MaterialTheme {
                AppNoticeHost {
                    Column(Modifier.fillMaxSize()) {
                        Text("Before", Modifier.testTag("before"))
                        TransientNotice(message.value, event.value)
                        Text("After", Modifier.testTag("after"))
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("before").fetchSemanticsNode().boundsInRoot
        val after = compose.onNodeWithTag("after").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { message.value = "Ошибка" }
        compose.onNodeWithTag("app-notice").assertIsDisplayed().performClick()
        compose.onNodeWithTag("app-notice").assertDoesNotExist()
        compose.runOnIdle { event.value++ }
        compose.onNodeWithTag("app-notice").assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("before").fetchSemanticsNode().boundsInRoot)
        assertEquals(after, compose.onNodeWithTag("after").fetchSemanticsNode().boundsInRoot)
    }

    @Test fun newMessageReplacesOldAndActionRunsOnce() {
        val message = mutableStateOf("first")
        var actions = 0
        compose.setContent {
            MaterialTheme {
                AppNoticeHost { TransientNotice(message.value, action = NoticeAction("Повторить") { actions++ }) }
            }
        }
        compose.runOnIdle { message.value = "second" }
        compose.onNodeWithText("first").assertDoesNotExist()
        assertEquals(1, compose.onAllNodesWithTag("app-notice").fetchSemanticsNodes().size)
        compose.onNodeWithText("Повторить").performClick()
        compose.onNodeWithTag("app-notice").assertDoesNotExist()
        assertEquals(1, actions)
    }

    @Test fun noticeDisappearsWithoutInteraction() {
        compose.mainClock.autoAdvance = false
        compose.setContent { MaterialTheme { AppNoticeHost { TransientNotice("Ок") } } }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("app-notice").assertExists()
        compose.mainClock.advanceTimeBy(120_000L)
        compose.onNodeWithTag("app-notice").assertDoesNotExist()
    }

    @Test fun compactStatusOnlyActionIsClickableAndDismisses() {
        var actions = 0
        compose.setContent {
            MaterialTheme {
                AppNoticeHost {
                    CompactStatusNotice("1 новое", 1, NoticeAction("Показать") { actions++ }, 12.dp, {})
                }
            }
        }
        compose.onNodeWithTag("app-notice").assertHasNoClickAction()
        compose.onNodeWithText("Показать").performClick()
        compose.onNodeWithTag("app-notice").assertDoesNotExist()
        assertEquals(1, actions)
    }

    @Test fun compactDeletionNoticeClosesOnItsOwn() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme { AppNoticeHost { CompactStatusNotice("0 новых, 1 удалён", 1, null, 12.dp, {}) } }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("app-notice").assertHasNoClickAction()
        compose.mainClock.advanceTimeBy(10_000)
        compose.onNodeWithTag("app-notice").assertDoesNotExist()
    }

    @Test fun bottomNoticeMovesAboveTheInset() {
        val bottom = mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                AppNoticeHost {
                    TransientNotice("Новое", bottomInset = if (bottom.value) 72.dp else null)
                }
            }
        }
        val top = compose.onNodeWithTag("app-notice").fetchSemanticsNode().boundsInRoot
        compose.runOnIdle { bottom.value = true }
        val lower = compose.onNodeWithTag("app-notice").fetchSemanticsNode().boundsInRoot
        assertTrue(lower.top < top.top)
        assertEquals(1, compose.onAllNodesWithTag("app-notice").fetchSemanticsNodes().size)
    }

    @Test fun changingInsetDoesNotDismissOrRepeatAnEvent() {
        val inset = mutableStateOf(12.dp)
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                AppNoticeHost {
                    TransientNotice("Событие", bottomInset = inset.value, onDismiss = { dismissals++ })
                }
            }
        }
        compose.onNodeWithTag("app-notice").assertIsDisplayed()
        compose.runOnIdle { inset.value = 72.dp }
        compose.runOnIdle { assertEquals(0, dismissals) }
        compose.onNodeWithTag("app-notice").performClick()
        compose.runOnIdle { inset.value = 12.dp }
        compose.onNodeWithTag("app-notice").assertDoesNotExist()
        compose.runOnIdle { assertEquals(1, dismissals) }
    }
}
