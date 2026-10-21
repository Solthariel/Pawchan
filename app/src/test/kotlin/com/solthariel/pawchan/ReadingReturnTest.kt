package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.settings.ReadingPreferences
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.infrastructure.thread.AppThreadReadingSession
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.thread.ThreadReadingPosition
import com.solthariel.pawchan.workspace.WorkspaceController
import com.solthariel.pawchan.workspace.requestPost
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ReadingReturnTest {
    private val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
    private val initial = WorkspaceState(tabs = listOf(OpenThread(address, "Thread", 150, 23)))

    @Test fun captureUsesImmediateViewportInsteadOfTheOlderDebouncedDiskPosition() =
        runTest {
            val storage = ReadingStorage(initial)
            val workspace = WorkspaceController(backgroundScope, storage)
            runCurrent()
            workspace.reading.position(address, 151, 47)
            workspace.requestPost(address, 300)
            assertEquals(300L, workspace.requestedPost?.postId)
            runCurrent()
            val session = AppThreadReadingSession(workspace)
            assertEquals(ThreadReadingPosition(151, 47), session.returnPoint(address).first())
            session.visible(address, ThreadReadingPosition(320, 12))
            session.persist(address, ThreadReadingPosition(320, 12))
            runCurrent()
            assertEquals(
                320L,
                storage.value.tabs
                    .single()
                    .postId,
            )
            assertEquals(ThreadReadingPosition(151, 47), session.returnPoint(address).first())
        }

    @Test fun queuedRepeatedJumpsAndReloadDoNotReplaceTheFirstAnchor() =
        runTest {
            val storage = ReadingStorage(initial)
            val workspace = WorkspaceController(backgroundScope, storage)
            runCurrent()
            workspace.requestPost(address, 300)
            workspace.reading.position(address, 320, 8)
            workspace.requestPost(address, 400)
            runCurrent()
            val restarted = WorkspaceController(backgroundScope, storage)
            runCurrent()
            val session = AppThreadReadingSession(restarted)
            assertEquals(ThreadReadingPosition(150, 23), session.returnPoint(address).first())
            session.returned(address, ThreadReadingPosition(150, 23))
            runCurrent()
            assertNull(session.returnPoint(address).first())
            assertEquals(
                150L,
                storage.value.tabs
                    .single()
                    .postId,
            )
            assertEquals(
                23,
                storage.value.tabs
                    .single()
                    .offset,
            )
            restarted.requestPost(address, 500)
            runCurrent()
            assertEquals(ThreadReadingPosition(150, 23), session.returnPoint(address).first())
        }

    @Test fun plainOpeningSamePostAndDisabledPositionMemoryCreateNoAnchor() =
        runTest {
            val storage = ReadingStorage(initial)
            val workspace = WorkspaceController(backgroundScope, storage)
            runCurrent()
            workspace.reading.position(address, 150, 0)
            workspace.requestPost(address, null)
            workspace.requestPost(address, 150)
            workspace.readingPreferences = ReadingPreferences(rememberPosition = false)
            workspace.requestPost(address, 300)
            runCurrent()
            assertNull(AppThreadReadingSession(workspace).returnPoint(address).first())
        }

    @Test fun oldPendingDebounceCannotOverwriteTheCompletedReturn() =
        runTest {
            val storage = ReadingStorage(initial)
            val workspace = WorkspaceController(backgroundScope, storage)
            runCurrent()
            workspace.requestPost(address, 300)
            runCurrent()
            val session = AppThreadReadingSession(workspace)
            val point = requireNotNull(session.returnPoint(address).first())
            val replyPosition = ThreadReadingPosition(320, 12)
            session.visible(address, replyPosition)
            session.returned(address, point)
            session.persist(address, replyPosition)
            runCurrent()
            assertEquals(
                150L,
                storage.value.tabs
                    .single()
                    .postId,
            )
            assertEquals(
                23,
                storage.value.tabs
                    .single()
                    .offset,
            )
            assertNull(session.returnPoint(address).first())
        }

    @Test fun outgoingAndForeignScreensCannotApplyOrConsumeTheNewJump() =
        runTest {
            val workspace = WorkspaceController(backgroundScope, ReadingStorage(initial))
            runCurrent()
            val outgoing = AppThreadReadingSession(workspace)
            workspace.requestPost(address, 300)
            assertEquals(ThreadReadingPosition(150, 23), outgoing.restore(address))
            outgoing.contentReady(address)
            assertEquals(300L, workspace.requestedPost?.postId)
            val incoming = AppThreadReadingSession(workspace)
            val foreign = address.copy(board = address.board.copy(site = requireNotNull(SiteId.parse("other"))))
            assertNull(incoming.restore(foreign))
            incoming.contentReady(foreign)
            assertEquals(ThreadReadingPosition(150, 23), incoming.restore(address))
            assertNull(outgoing.pendingPost(address))
            assertNull(incoming.pendingPost(foreign))
            assertEquals(300L, incoming.pendingPost(address))
            assertEquals(300L, workspace.requestedPost?.postId)
            incoming.contentReady(address)
            assertEquals(300L, workspace.requestedPost?.postId)
            incoming.clearPendingPost(foreign)
            assertEquals(300L, incoming.pendingPost(address))
            incoming.clearPendingPost(address)
            assertNull(workspace.requestedPost)
            assertEquals(ThreadReadingPosition(150, 23), incoming.restore(address))
        }

    @Test fun failedReturnWriteKeepsThePersistentAnchorForRetry() =
        runTest {
            val storage = ReadingStorage(initial)
            val workspace = WorkspaceController(backgroundScope, storage)
            runCurrent()
            workspace.requestPost(address, 300)
            runCurrent()
            val session = AppThreadReadingSession(workspace)
            val point = requireNotNull(session.returnPoint(address).first())
            storage.fail = true
            session.returned(address, point)
            runCurrent()
            assertEquals(point, session.returnPoint(address).first())
            storage.fail = false
            session.returned(address, point)
            runCurrent()
            assertNull(session.returnPoint(address).first())
            assertEquals(
                point.postId,
                storage.value.tabs
                    .single()
                    .postId,
            )
        }

    @Test fun sourcePositionCannotLeakToAnotherSiteOrUnvisitedThread() =
        runTest {
            val storage = ReadingStorage(initial)
            val workspace = WorkspaceController(backgroundScope, storage)
            runCurrent()
            val foreign = address.copy(board = address.board.copy(site = requireNotNull(SiteId.parse("other"))))
            workspace.requestPost(foreign, 300)
            runCurrent()
            assertNull(AppThreadReadingSession(workspace).returnPoint(foreign).first())
            assertNull(AppThreadReadingSession(workspace).returnPoint(address).first())
        }
}

private class ReadingStorage(
    var value: WorkspaceState,
) : WorkspaceRepository {
    var fail = false

    override suspend fun load(): WorkspaceState = value

    override suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState {
        if (fail) throw IOException("Disk unavailable")
        value = transform(value)
        return value
    }
}
