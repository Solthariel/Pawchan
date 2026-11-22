package com.solthariel.pawchan

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.arhivach.ArchiveThreadCardUi
import com.solthariel.pawchan.arhivach.rememberArchiveThreadListState
import com.solthariel.pawchan.domain.archive.ArchiveThreadSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArchiveScrollRetentionRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun returningFromThreadKeepsPositionAndNewFeedResetsIt() {
        var showingThread by mutableStateOf(false)
        var feedKey by mutableIntStateOf(1)
        lateinit var listState: LazyListState
        val cards = cards()
        compose.setContent {
            val archive = rememberArchiveThreadListState(feedKey, cards, "")
            listState = archive.listState
            if (showingThread) {
                Text("Открытый тред")
            } else {
                LazyColumn(state = archive.listState, modifier = Modifier.height(240.dp).testTag("archive-list")) {
                    items(archive.shown, key = { it.summary.id }) { card ->
                        Text(card.summary.title, Modifier.height(48.dp))
                    }
                }
            }
        }
        compose.onNodeWithTag("archive-list").performScrollToIndex(80)
        compose.runOnIdle { assertEquals(80, listState.firstVisibleItemIndex) }
        compose.runOnIdle { showingThread = true }
        compose.onNodeWithText("Открытый тред").assertIsDisplayed()
        compose.runOnIdle { showingThread = false }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(80, listState.firstVisibleItemIndex) }
        compose.runOnIdle { showingThread = true }
        compose.runOnIdle { feedKey++ }
        compose.waitForIdle()
        compose.runOnIdle { showingThread = false }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(0, listState.firstVisibleItemIndex) }
    }

    @Test fun filteredCardsRemainAvailableAfterThreadIsClosed() {
        var showingThread by mutableStateOf(false)
        var words by mutableStateOf("тред 42")
        var shownSize = 0
        val cards = cards()
        compose.setContent {
            val archive = rememberArchiveThreadListState(1, cards, words)
            shownSize = archive.shown.size
            if (showingThread) {
                Text("Открытый тред")
            } else {
                LazyColumn(Modifier.height(240.dp), state = archive.listState) {
                    items(archive.shown, key = { it.summary.id }) { Text(it.summary.title) }
                }
            }
        }
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Тред 42").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Тред 42").assertIsDisplayed()
        compose.runOnIdle { showingThread = true }
        compose.runOnIdle { showingThread = false }
        compose.onNodeWithText("Тред 42").assertIsDisplayed()
        compose.runOnIdle { words = "" }
        compose.runOnIdle { assertEquals(100, shownSize) }
        compose.onNodeWithText("Тред 42").assertIsDisplayed()
    }

    private fun cards(): List<ArchiveThreadCardUi> =
        (0L until 100L).map { id ->
            ArchiveThreadCardUi(ArchiveThreadSummary(id, "Тред $id", 1, null, emptyList()), "", emptyList())
        }
}
