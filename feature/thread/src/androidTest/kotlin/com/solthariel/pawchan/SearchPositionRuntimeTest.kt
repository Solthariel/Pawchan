package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.thread.BindThreadScreenEffects
import com.solthariel.pawchan.thread.ThreadActions
import com.solthariel.pawchan.thread.ThreadExportActions
import com.solthariel.pawchan.thread.ThreadScreenMemory
import com.solthariel.pawchan.thread.ThreadSearchBar
import com.solthariel.pawchan.thread.ThreadSearchFlag
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.rememberThreadScreenMemory
import com.solthariel.pawchan.thread.toThreadUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SearchPositionRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var memory: ThreadScreenMemory
    private val content = testContent()

    @Test fun openingTypingChangingAdvancedFlagsAndClosingNeverResetTheViewport() {
        show()
        compose.onNodeWithTag("search-list").performScrollToIndex(20)
        compose.runOnIdle { memory.tools.searchOpen = true }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        compose.waitUntil(5_000) { memory.tools.results.isNotEmpty() }
        compose.runOnIdle { assertEquals(20, memory.list.firstVisibleItemIndex) }
        compose.runOnIdle { memory.tools.search(content, "hit") }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        compose.waitUntil(5_000) { memory.tools.results.size == 8 }
        compose.runOnIdle { assertEquals(20, memory.list.firstVisibleItemIndex) }
        compose.runOnIdle { memory.tools.setFlag(ThreadSearchFlag.OP, content) }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        compose.waitUntil(5_000) { !memory.tools.searching }
        compose.runOnIdle {
            assertEquals(20, memory.list.firstVisibleItemIndex)
            memory.tools.closeSearch()
        }
        compose.runOnIdle { assertEquals(20, memory.list.firstVisibleItemIndex) }
    }

    @Test fun searchArrowsNavigateFromTheVisiblePostRatherThanTheFirstMatch() {
        show()
        compose.onNodeWithTag("search-list").performScrollToIndex(20)
        compose.runOnIdle { memory.tools.searchOpen = true }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        compose.waitUntil(5_000) { memory.tools.results.isNotEmpty() }
        compose.runOnIdle { memory.tools.search(content, "hit") }
        compose.mainClock.advanceTimeBy(300)
        compose.waitForIdle()
        compose.waitUntil(5_000) { memory.tools.results.size == 8 }
        compose.onNodeWithContentDescription("Следующее совпадение").performClick()
        compose.runOnIdle { assertEquals(29, memory.list.firstVisibleItemIndex) }
        compose.onNodeWithContentDescription("Предыдущее совпадение").performClick()
        compose.runOnIdle { assertEquals(19, memory.list.firstVisibleItemIndex) }
    }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                memory = rememberThreadScreenMemory(content, content.address, null)
                BindThreadScreenEffects(memory, content, ThreadActions({}, {}, {}, {}), ThreadExportActions({}, {}))
                Column(Modifier.fillMaxSize()) {
                    if (memory.tools.searchOpen) {
                        ThreadSearchBar(memory.tools, content) { memory.moveSearch(content, it) }
                    } else {
                        Text("Toolbar", Modifier.height(36.dp))
                    }
                    LazyColumn(Modifier.weight(1f).testTag("search-list"), state = memory.list) {
                        items(80) { Text("Post $it", Modifier.fillMaxWidth().height(48.dp)) }
                    }
                }
            }
        }
    }

    private fun testContent(): ThreadUiState.Content {
        val address = defaultThreadAddress(1)
        val posts =
            (1L..80L).map { id ->
                BoardPost(
                    PostAddress(address, requireNotNull(PostId.parse(id))),
                    PostContent(PostDocument.plain(if (id % 10 == 0L) "hit" else "other"), PostMetadata(null, null, false)),
                    emptyList(),
                )
            }
        return DomainOutcome
            .Success(ThreadSnapshot(address, "Test", ThreadFlags(false, false, false), posts))
            .toThreadUiState() as ThreadUiState.Content
    }
}
