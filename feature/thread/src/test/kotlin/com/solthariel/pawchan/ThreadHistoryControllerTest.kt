package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadHistoryRepository
import com.solthariel.pawchan.thread.ThreadHistoryController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ThreadHistoryControllerTest {
    private val address = defaultThreadAddress(1)

    @Test fun historyEditsWaitForTheSharedMutexAndRetainFavoriteState() =
        runTest {
            val stored = snapshot(listOf(1, 2)).let { it.withPosts(it.posts, setOf(requireNotNull(PostId.parse(2)))) }
            val history = RecordingHistory(stored)
            val mutex = Mutex(locked = true)
            var shown: ThreadSnapshot? = null
            var favorite = false
            val current = toolsContent(listOf(toolsPost(1), toolsPost(2)))
            val actions =
                ThreadHistoryController(
                    this,
                    address,
                    history,
                    mutex,
                    { current.copy(presentation = current.presentation.copy(isFavorite = true)) },
                    { result, selected ->
                        shown = result
                        favorite = selected
                    },
                ).actions {}
            actions.clearDeleted()
            runCurrent()
            assertTrue(history.events.isEmpty())
            mutex.unlock()
            runCurrent()
            assertEquals(listOf("load", "save"), history.events)
            assertEquals(listOf(1L), shown?.posts?.map { it.address.post.value })
            assertTrue(favorite)
        }

    @Test fun restoreReportsTheAddedCountAndEraseRefreshesOnlyAfterStorage() =
        runTest {
            val history = RecordingHistory(snapshot(listOf(1)))
            var added = -1
            var refreshed = false
            val actions =
                ThreadHistoryController(
                    this,
                    address,
                    history,
                    Mutex(),
                    { toolsContent(listOf(toolsPost(1))) },
                    { _, _ -> },
                ).actions {
                    assertEquals("erase", history.events.last())
                    refreshed = true
                }
            actions.restore(snapshot(listOf(1, 3))) { added = it }
            runCurrent()
            assertEquals(1, added)
            assertEquals(setOf(requireNotNull(PostId.parse(3))), history.value?.deletedPosts)
            actions.erase()
            assertFalse(refreshed)
            runCurrent()
            assertTrue(refreshed)
            assertEquals(null, history.value)
        }

    @Test fun cancelledOwnerDoesNotRunPendingHistoryActions() =
        runTest {
            val history = RecordingHistory(snapshot(listOf(1)))
            val job = Job()
            val scope = CoroutineScope(coroutineContext + job)
            var refreshed = false
            val mutex = Mutex(locked = true)
            val actions =
                ThreadHistoryController(
                    scope,
                    address,
                    history,
                    mutex,
                    { toolsContent(listOf(toolsPost(1))) },
                    { _, _ -> },
                ).actions { refreshed = true }
            actions.erase()
            runCurrent()
            job.cancel()
            mutex.unlock()
            runCurrent()
            assertTrue(history.events.isEmpty())
            assertFalse(refreshed)
        }

    private fun snapshot(ids: List<Long>): ThreadSnapshot =
        ThreadSnapshot(
            address,
            "history",
            ThreadFlags(false, false, false),
            ids.map { id ->
                BoardPost(
                    PostAddress(address, requireNotNull(PostId.parse(id))),
                    PostContent(PostDocument.plain("post $id"), PostMetadata(null, null, false)),
                    emptyList(),
                )
            },
        )
}

private class RecordingHistory(
    var value: ThreadSnapshot?,
) : ThreadHistoryRepository {
    val events = mutableListOf<String>()

    override suspend fun load(address: ThreadAddress): ThreadSnapshot? {
        events += "load"
        return value
    }

    override suspend fun save(snapshot: ThreadSnapshot) {
        events += "save"
        value = snapshot
    }

    override suspend fun erase(address: ThreadAddress) {
        events += "erase"
        value = null
    }
}
