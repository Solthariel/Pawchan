package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadHistoryRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceRepository
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.workspace.WorkspaceController
import com.solthariel.pawchan.workspace.WorkspaceThreadRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceMissingThreadTest {
    private val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
    private val snapshot = ThreadSnapshot(address, "Тред", ThreadFlags(false, false, false), emptyList())
    private val history =
        object : ThreadHistoryRepository {
            override suspend fun load(address: ThreadAddress): ThreadSnapshot = snapshot

            override suspend fun save(snapshot: ThreadSnapshot) = Unit

            override suspend fun erase(address: ThreadAddress) = error("History must not be erased")
        }
    private val storage =
        object : WorkspaceRepository {
            override suspend fun load(): WorkspaceState = WorkspaceState()

            override suspend fun update(transform: (WorkspaceState) -> WorkspaceState): WorkspaceState {
                val initial = WorkspaceState()
                return transform(initial)
            }
        }

    @Test fun missingThreadIsNotReportedAsSuccessfulCacheRefresh() =
        runTest {
            val workspace = WorkspaceController(backgroundScope, storage, history)
            val failure = DomainOutcome.Failure(DomainFailure.NotFound)
            val repository = WorkspaceThreadRepository(remote(failure), workspace)
            assertSame(failure, repository.loadThread(address))
            assertSame(snapshot, repository.cachedThread(address))
            assertSame(snapshot, history.load(address))
        }

    @Test fun ordinaryNetworkFailureStillUsesTheExistingOfflineFallback() =
        runTest {
            val workspace = WorkspaceController(backgroundScope, storage, history)
            val failure = DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
            val repository = WorkspaceThreadRepository(remote(failure), workspace)
            val loaded = repository.loadThread(address)
            assertTrue(loaded is DomainOutcome.Success)
            assertSame(snapshot, (loaded as DomainOutcome.Success).value)
        }

    @Test fun missingThreadWithoutLocalDataRemainsAFailure() =
        runTest {
            val failure = DomainOutcome.Failure(DomainFailure.NotFound)
            assertSame(failure, WorkspaceThreadRepository(remote(failure), null).loadThread(address))
        }

    private fun remote(result: DomainOutcome<ThreadSnapshot>): ThreadRepository =
        object : ThreadRepository {
            override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> = result
        }
}
