package com.solthariel.pawchan

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.arhivach.ArchiveRefreshHost
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArchiveRefreshOverlayRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun toolbarRefreshDoesNotUseSwipeOverlayOrMoveLoadedRows() {
        var refreshing by mutableStateOf(false)
        compose.setContent {
            MaterialTheme {
                ArchiveRefreshHost(refreshing, {}) {
                    LazyColumn(Modifier.height(400.dp).testTag("archive-list")) {
                        items(100) { Text("Тред $it", Modifier.height(48.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("archive-list").performScrollToIndex(40)
        val rowTop =
            compose
                .onNodeWithText("Тред 40")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot.top
        compose.runOnIdle { refreshing = true }
        compose.onNodeWithContentDescription("Обновление сверху").assertDoesNotExist()
        val during =
            compose
                .onNodeWithText("Тред 40")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertEquals(rowTop, during, 0f)
        compose.runOnIdle { refreshing = false }
        compose.onNodeWithContentDescription("Обновление сверху").assertDoesNotExist()
        val after =
            compose
                .onNodeWithText("Тред 40")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertEquals(rowTop, after, 0f)
    }

    @Test fun pullingFromTopUsesSameOverlayAndStartsOneRefresh() {
        var refreshing by mutableStateOf(false)
        var requests = 0
        compose.setContent {
            MaterialTheme {
                ArchiveRefreshHost(refreshing, {
                    requests++
                    refreshing = true
                }) {
                    LazyColumn(Modifier.height(400.dp).testTag("archive-list")) {
                        items(100) { Text("Тред $it", Modifier.height(48.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("archive-list").performTouchInput {
            down(Offset(center.x, height * 0.1f))
            moveTo(Offset(center.x, height * 0.85f), delayMillis = 500)
            up()
        }
        compose.onNodeWithContentDescription("Обновление сверху").assertExists()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.runOnIdle { refreshing = false }
        compose.onNodeWithContentDescription("Обновление сверху").assertDoesNotExist()
    }

    @Test fun pullingFromBottomKeepsBottomIndicator() {
        var refreshing by mutableStateOf(false)
        var requests = 0
        compose.setContent {
            MaterialTheme {
                ArchiveRefreshHost(refreshing, {
                    requests++
                    refreshing = true
                }) {
                    LazyColumn(Modifier.height(400.dp).testTag("archive-list")) {
                        items(100) { Text("Тред $it", Modifier.height(48.dp)) }
                    }
                }
            }
        }
        compose.onNodeWithTag("archive-list").performScrollToIndex(99)
        compose.onNodeWithTag("archive-list").performTouchInput {
            down(Offset(center.x, height * 0.9f))
            moveTo(Offset(center.x, height * 0.15f), delayMillis = 500)
            up()
        }
        compose.onNodeWithContentDescription("Обновление снизу").assertExists()
        compose.runOnIdle { assertEquals(1, requests) }
        compose.runOnIdle { refreshing = false }
        compose.onNodeWithContentDescription("Обновление снизу").assertDoesNotExist()
    }

    @Test fun initialLoadingDoesNotShowSwipeOverlay() {
        compose.setContent {
            MaterialTheme {
                ArchiveRefreshHost(refreshing = true, onRefresh = {}) { Text("Содержимое") }
            }
        }
        compose.onNodeWithContentDescription("Обновление сверху").assertDoesNotExist()
    }
}
