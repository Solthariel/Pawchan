package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadHistoryRepository
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.navigation.threadAddress
import com.solthariel.pawchan.threads.FavoriteThreadsWatch
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteDeletedThreadTest {
    @Test
    fun confirmedDeletionIsNotRecheckedOrReplacedByANetworkError() =
        runTest {
            val address = requireNotNull(threadAddress("dvach", "b", 42L))
            var calls = 0
            var reason: DomainFailure = DomainFailure.NotFound
            val remote =
                object : ThreadRepository {
                    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                        calls++
                        return DomainOutcome.Failure(reason)
                    }
                }
            val history =
                object : ThreadHistoryRepository {
                    override suspend fun load(address: ThreadAddress): ThreadSnapshot? = null

                    override suspend fun save(snapshot: ThreadSnapshot) = error("No successful refresh")

                    override suspend fun erase(address: ThreadAddress) = error("Must keep existing history")
                }
            val watch = FavoriteThreadsWatch(backgroundScope, history, remote, null)
            watch.refresh(listOf(address))
            runCurrent()
            val missing = watch.counters.getValue(address)
            assertEquals("×", missing.cellLabel)
            assertEquals(true, missing.deleted)
            assertEquals(false, missing.error)
            assertEquals(false, missing.running)
            reason = DomainFailure.NetworkUnavailable
            watch.refresh(listOf(address))
            runCurrent()
            val network = watch.counters.getValue(address)
            assertEquals("×", network.cellLabel)
            assertEquals(true, network.deleted)
            assertEquals(false, network.error)
            assertEquals(1, calls)
            assertEquals(false, network.running)
        }
}
