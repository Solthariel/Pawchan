package com.solthariel.pawchan

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.thread.BindThreadPendingPost
import com.solthariel.pawchan.thread.BindThreadReadingReturn
import com.solthariel.pawchan.thread.ThreadOverflowMenu
import com.solthariel.pawchan.thread.ThreadReadingPosition
import com.solthariel.pawchan.thread.ThreadReadingSession
import com.solthariel.pawchan.thread.ThreadScreenMemory
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.readingReturnMarker
import com.solthariel.pawchan.thread.readingReturnScrollMarkers
import com.solthariel.pawchan.thread.rememberThreadScreenMemory
import com.solthariel.pawchan.thread.toThreadUiState
import com.solthariel.pawchan.thread.withPostNavigation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class ReadingReturnRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var scope: CoroutineScope
    private lateinit var memory: ThreadScreenMemory
    private val session = ReturnTestSession()
    private var content by mutableStateOf(testContent())
    private var requests = 0

    @Test fun menuReturnsToStablePostAndExactOffsetThenRemovesBothMarkers() {
        show()
        compose.runOnIdle {
            assertEquals(59, memory.list.firstVisibleItemIndex)
            assertEquals(9, content.readingReturnScrollMarkers(memory.tools.readingReturn).reading)
            memory.tools.menu = true
        }
        compose.onNodeWithText("Вернуться к месту чтения").performClick()
        compose.runOnIdle {
            assertEquals(9, memory.list.firstVisibleItemIndex)
            assertEquals(13, memory.list.firstVisibleItemScrollOffset)
            assertNull(session.point.value)
            assertNull(memory.tools.readingReturn)
            assertNull(content.readingReturnScrollMarkers(memory.tools.readingReturn).reading)
        }
    }

    @Test fun unavailablePostDoesNotJumpToTheBeginningOrDiscardTheSavedPoint() {
        show()
        compose.runOnIdle {
            session.point.value = ThreadReadingPosition(999, 13)
        }
        compose.waitForIdle()
        compose.runOnIdle { memory.tools.menu = true }
        compose.onNodeWithText("Вернуться к месту чтения").performClick()
        compose.runOnIdle {
            assertEquals(59, memory.list.firstVisibleItemIndex)
            assertEquals(ThreadReadingPosition(999, 13), session.point.value)
            assertEquals("Место чтения недоступно в загруженном треде", memory.tools.notice)
        }
    }

    @Test fun interruptedScrollKeepsTheReturnPointForRetry() {
        show()
        lateinit var blocking: Job
        compose.runOnIdle {
            blocking =
                scope.launch {
                    memory.list.scroll(MutatePriority.PreventUserInput) { awaitCancellation() }
                }
        }
        compose.runOnIdle { memory.tools.menu = true }
        compose.onNodeWithText("Вернуться к месту чтения").performClick()
        compose.runOnIdle {
            assertEquals(59, memory.list.firstVisibleItemIndex)
            assertEquals(ThreadReadingPosition(10, 13), session.point.value)
            blocking.cancel()
        }
        compose.runOnIdle { memory.tools.menu = true }
        compose.onNodeWithText("Вернуться к месту чтения").performClick()
        compose.runOnIdle {
            assertEquals(9, memory.list.firstVisibleItemIndex)
            assertNull(session.point.value)
        }
    }

    @Test fun absentReplyKeepsViewportUntilItArrivesWithoutRepeatedRequests() {
        session.pending = 70
        content = content.copy(posts = content.posts.filter { it.id != 70L }).withPostNavigation()
        show()
        compose.runOnIdle {
            assertEquals(59, memory.list.firstVisibleItemIndex)
            assertEquals(1, requests)
            content = content.copy(subject = "Changed")
        }
        compose.runOnIdle {
            assertEquals(1, requests)
            content = testContent()
        }
        compose.runOnIdle {
            assertEquals(69, memory.list.firstVisibleItemIndex)
            assertEquals(70L, content.posts[memory.list.firstVisibleItemIndex].id)
            assertEquals(0, memory.list.firstVisibleItemScrollOffset)
            assertNull(session.pending)
        }
    }

    @Test fun explicitReturnCancelsPendingReplyBeforeItsLaterArrival() {
        session.pending = 70
        content = content.copy(posts = content.posts.filter { it.id != 70L }).withPostNavigation()
        show()
        compose.runOnIdle { memory.tools.menu = true }
        compose.onNodeWithText("Вернуться к месту чтения").performClick()
        compose.runOnIdle {
            assertEquals(9, memory.list.firstVisibleItemIndex)
            assertNull(session.pending)
            content = testContent()
        }
        compose.runOnIdle { assertEquals(9, memory.list.firstVisibleItemIndex) }
    }

    private fun show() {
        compose.setContent {
            MaterialTheme {
                scope = rememberCoroutineScope()
                memory = rememberThreadScreenMemory(content, content.address, session)
                BindThreadPendingPost(memory, content, content.address, session) { requests++ }
                BindThreadReadingReturn(memory, content, content.address, session)
                LazyColumn(Modifier.fillMaxSize().testTag("return-list"), state = memory.list) {
                    items(content.posts, key = { it.id }) { post ->
                        Text(
                            "Post ${post.id}",
                            Modifier.fillMaxWidth().height(48.dp).readingReturnMarker(
                                post.id == memory.tools.readingReturn?.postId,
                            ),
                        )
                    }
                }
                ThreadOverflowMenu(memory.tools, content)
            }
        }
        compose.waitForIdle()
    }

    private fun testContent(): ThreadUiState.Content {
        val address = defaultThreadAddress(1)
        val posts =
            (1L..120L).map { id ->
                BoardPost(
                    PostAddress(address, requireNotNull(PostId.parse(id))),
                    PostContent(PostDocument.plain("Post $id"), PostMetadata(null, null, false)),
                    emptyList(),
                )
            }
        return DomainOutcome
            .Success(
                ThreadSnapshot(address, "Test", ThreadFlags(false, false, false), posts),
            ).toThreadUiState() as ThreadUiState.Content
    }
}

private class ReturnTestSession : ThreadReadingSession {
    var pending: Long? = null

    override fun pendingPost(address: ThreadAddress): Long? = pending

    override fun clearPendingPost(address: ThreadAddress) {
        pending = null
    }

    val point = MutableStateFlow<ThreadReadingPosition?>(ThreadReadingPosition(10, 13))

    override fun restore(address: ThreadAddress): ThreadReadingPosition = ThreadReadingPosition(60, 0)

    override fun returnPoint(address: ThreadAddress): MutableStateFlow<ThreadReadingPosition?> = point

    override fun returned(
        address: ThreadAddress,
        point: ThreadReadingPosition,
    ) {
        if (this.point.value == point) this.point.value = null
    }

    override fun contentReady(address: ThreadAddress) = Unit

    override fun visible(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    ) = Unit

    override fun persist(
        address: ThreadAddress,
        position: ThreadReadingPosition,
    ) = Unit
}
