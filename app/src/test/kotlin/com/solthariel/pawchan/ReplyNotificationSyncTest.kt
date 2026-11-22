package com.solthariel.pawchan

import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.workspace.StoredReply
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.domain.workspace.markRead
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.workspace.ReplyNotificationSync
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ReplyNotificationSyncTest {
    @get:Rule val files = TemporaryFolder()

    @Test fun reconciliationCountsOnlyUnreadRepliesFromTheSelectedThread() {
        val thread =
            com.solthariel.pawchan.domain.model
                .ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
        val other = thread.copy(thread = requireNotNull(ThreadId.parse(99)))
        val one = reply(thread, 43)
        val two = reply(thread, 44)
        val elsewhere = reply(other, 100)
        val calls = mutableListOf<Int>()
        val sync = ReplyNotificationSync(MaintenanceGate(files.newFolder("gate"))) { _, unread -> calls += unread }
        val replies = listOf(StoredReply(one, null), StoredReply(two, null), StoredReply(elsewhere, null))
        var state = WorkspaceState(replies = replies)
        sync.reconcile(thread, state)
        state = state.markRead(setOf(one.address))
        sync.reconcile(thread, state)
        state = state.markRead(setOf(two.address))
        sync.reconcile(thread, state)
        assertEquals(listOf(2, 1, 0), calls)
        assertEquals(true, state.replies.last().unread)
    }

    @Test fun independentInstancesSerializeWorkerAndReaderTransactions() =
        runBlocking {
            withTimeout(5_000) {
                val dir = File(files.root, "shared-gate")
                val first = ReplyNotificationSync(MaintenanceGate(dir)) { _, _ -> }
                val second = ReplyNotificationSync(MaintenanceGate(dir)) { _, _ -> }
                val entered = CompletableDeferred<Unit>()
                val release = CompletableDeferred<Unit>()
                val secondStarted = CompletableDeferred<Unit>()
                val events = mutableListOf<String>()
                val writer =
                    async(Dispatchers.Default) {
                        first.access {
                            events += "worker starts"
                            entered.complete(Unit)
                            release.await()
                            events += "worker ends"
                        }
                    }
                entered.await()
                val reader =
                    async(Dispatchers.Default) {
                        secondStarted.complete(Unit)
                        second.access { events += "reader" }
                    }
                secondStarted.await()
                delay(100)
                assertFalse(reader.isCompleted)
                release.complete(Unit)
                writer.await()
                reader.await()
                assertEquals(listOf("worker starts", "worker ends", "reader"), events)
            }
        }

    private fun reply(
        thread: com.solthariel.pawchan.domain.model.ThreadAddress,
        id: Long,
    ): BoardPost =
        BoardPost(
            PostAddress(thread, requireNotNull(PostId.parse(id))),
            PostContent(PostDocument(emptyList()), PostMetadata(null, null, false)),
            emptyList(),
        )
}
