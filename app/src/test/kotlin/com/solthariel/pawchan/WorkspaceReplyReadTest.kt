package com.solthariel.pawchan

import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.workspace.StoredReply
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.workspace.ReplyNotificationSync
import com.solthariel.pawchan.workspace.WorkspaceController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class WorkspaceReplyReadTest {
    @get:Rule val files = TemporaryFolder()

    @Test fun enteringDoesNotReadAndVisiblePostsUpdateOnlyTheirOwnThread() =
        runBlocking {
            withTimeout(5_000) {
                val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
                val other = address.copy(thread = requireNotNull(ThreadId.parse(99)))
                val one = post(address, 43)
                val two = post(address, 44)
                val elsewhere = post(other, 43)
                val replies = listOf(StoredReply(one, null), StoredReply(two, null), StoredReply(elsewhere, null))
                val initial = WorkspaceState(replies = replies)
                val repository = ReplyReadStorage(initial)
                val counts = java.util.Collections.synchronizedList(mutableListOf<Int>())
                val gate = MaintenanceGate(files.newFolder("gate"))
                val sync = ReplyNotificationSync(gate) { _, count -> counts += count }
                val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
                try {
                    val controller = WorkspaceController(scope, repository, replyNotifications = sync)
                    controller.state.first { it.replies.size == 3 }
                    val snapshot = ThreadSnapshot(address, "test", ThreadFlags(false, false, false), listOf(one, two))
                    controller.observe(snapshot)
                    val entered = controller.state.first { it.tabs.any { tab -> tab.address == address } }
                    assertTrue(entered.replies.all { it.unread })
                    controller.markVisibleReplies(address, setOf(43))
                    val partlyRead =
                        controller.state.first { state ->
                            state.replies
                                .first { it.post.address == one.address }
                                .unread
                                .not()
                        }
                    assertTrue(partlyRead.replies.first { it.post.address == two.address }.unread)
                    assertTrue(partlyRead.replies.first { it.post.address == elsewhere.address }.unread)
                    controller.markVisibleReplies(address, setOf(44))
                    controller.state.first { state ->
                        state.replies
                            .first { it.post.address == two.address }
                            .unread
                            .not()
                    }
                    assertEquals(listOf(2, 1, 0), counts.toList())
                } finally {
                    scope.cancel()
                }
            }
        }

    private fun post(
        address: ThreadAddress,
        id: Long,
    ): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(id))),
            PostContent(PostDocument(emptyList()), PostMetadata(null, null, false)),
            emptyList(),
        )
}

private class ReplyReadStorage(
    initial: WorkspaceState,
) : WorkspaceRepository {
    private var state = initial

    override suspend fun load(): WorkspaceState = synchronized(this) { state }

    override suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState =
        synchronized(this) {
            state = transform(state)
            state
        }
}
