package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.watch.PersistThreadWatchResult
import com.solthariel.pawchan.domain.watch.ThreadWatchRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchResult
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class WatchResultPersistenceTest {
    private val address = ThreadAddress(defaultBoardAddress(), requireNotNull(ThreadId.parse(42)))
    private val checkpoint = requireNotNull(PostId.parse(43))

    @Test
    fun closedResultAttemptsDisableEvenWhenCheckpointFails() =
        runTest {
            val calls = mutableListOf<String>()
            val watches =
                object : ThreadWatchRepository by FakeBackgroundWatches() {
                    override suspend fun updateCheckpoint(
                        address: ThreadAddress,
                        lastKnownPostId: PostId,
                    ): DomainOutcome<Unit> {
                        calls += "checkpoint"
                        return DomainOutcome.Failure(DomainFailure.StorageUnavailable)
                    }

                    override suspend fun disable(address: ThreadAddress): DomainOutcome<Unit> {
                        calls += "disable"
                        return DomainOutcome.Success(Unit)
                    }
                }
            val snapshot = ThreadSnapshot(address, null, ThreadFlags(false, true, false), emptyList())
            val result =
                PersistThreadWatchResult(watches)(address, ThreadWatchResult.Closed(snapshot, emptyList(), checkpoint))
            assertEquals(listOf("checkpoint", "disable"), calls)
            assertEquals(DomainOutcome.Failure(DomainFailure.StorageUnavailable), result)
        }

    @Test
    fun deletedResultDisablesWithoutCheckpoint() =
        runTest {
            var saves = 0
            var stops = 0
            val watches =
                object : ThreadWatchRepository by FakeBackgroundWatches() {
                    override suspend fun updateCheckpoint(
                        address: ThreadAddress,
                        lastKnownPostId: PostId,
                    ): DomainOutcome<Unit> {
                        saves++
                        return DomainOutcome.Success(Unit)
                    }

                    override suspend fun disable(address: ThreadAddress): DomainOutcome<Unit> {
                        stops++
                        return DomainOutcome.Failure(DomainFailure.StorageUnavailable)
                    }
                }
            assertEquals(
                DomainOutcome.Failure(DomainFailure.StorageUnavailable),
                PersistThreadWatchResult(watches)(address, ThreadWatchResult.Deleted),
            )
            assertEquals(0, saves)
            assertEquals(1, stops)
        }
}
