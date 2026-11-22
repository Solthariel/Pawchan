package com.solthariel.pawchan.thread

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class ReplyVisibilityRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42)),
        )

    @Test fun scrollingMarksOnlyTheReplyActuallyShown() {
        val session = VisibleReadingSession(setOf(43, 44))
        showThread(session, mutableStateOf(true))
        compose.onNodeWithTag("reply-list").performScrollToIndex(1)
        compose.waitUntil(5_000) { 43L in session.read }
        assertEquals(listOf(43L), session.read.toList())
        compose.onNodeWithTag("reply-list").performScrollToIndex(2)
        compose.waitUntil(5_000) { 44L in session.read }
        assertEquals(listOf(43L, 44L), session.read.toList())
    }

    @Test fun fullscreenComposerDoesNotReadTheCoveredThread() {
        val session = VisibleReadingSession(setOf(42))
        val visible = mutableStateOf(false)
        showThread(session, visible)
        compose.waitForIdle()
        Thread.sleep(700)
        assertTrue(session.read.isEmpty())
        compose.runOnIdle { visible.value = true }
        compose.waitUntil(5_000) { 42L in session.read }
        assertEquals(listOf(42L), session.read.toList())
    }

    private fun showThread(
        session: VisibleReadingSession,
        visible: MutableState<Boolean>,
    ) {
        val content = content()
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalThreadReadingEnabled provides visible.value) {
                    val memory = rememberThreadScreenMemory(content, address, session)
                    BindVisibleReplies(memory, content, address, session)
                    LazyColumn(Modifier.height(100.dp).testTag("reply-list"), state = memory.list) {
                        items(content.posts, key = { it.id }) { post ->
                            Text("Post ${post.id}", Modifier.fillMaxWidth().height(100.dp))
                        }
                    }
                }
            }
        }
    }

    private fun content(): ThreadUiState.Content {
        val document = PostDocument(emptyList())
        val posts =
            listOf(42L, 43L, 44L).map { id ->
                ThreadPostUi(id, document, RichPostTextUi.create(document), ThreadPostMetadataUi(null, null, false), emptyList())
            }
        val positions = posts.mapIndexed { index, post -> post.id to index }.toMap()
        return ThreadUiState.Content(
            id = 42,
            subject = null,
            posts = posts,
            flags = ThreadContentFlags(false),
            presentation = ThreadContentPresentation(false, navigation = ThreadPostNavigation(postPositions = positions)),
            statistics = ThreadContentStatistics(),
            address = address,
        )
    }
}

private class VisibleReadingSession(
    ids: Set<Long>,
) : ThreadReadingSession {
    val read = CopyOnWriteArrayList<Long>()
    private val unread = MutableStateFlow(ids)

    override fun unreadReplies(address: ThreadAddress) = unread

    override fun markVisibleReplies(
        address: ThreadAddress,
        postIds: Set<Long>,
    ) {
        read.addAll(postIds.sorted())
        unread.value -= postIds
    }

    override fun restore(address: ThreadAddress): ThreadReadingPosition? = null

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
