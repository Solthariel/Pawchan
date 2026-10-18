package com.solthariel.pawchan.domain.thread

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadCheckPassRepositoryTest {
    @Test
    fun sharesFailureOnlyWithinOnePass() =
        runBlocking {
            var loads = 0
            val source =
                PassSource {
                    loads++
                    DomainOutcome.Failure(DomainFailure.NotFound)
                }
            val pass = ThreadCheckPassRepository(source)
            pass.loadThread(address())
            pass.loadThread(address())
            assertEquals(1, loads)
            ThreadCheckPassRepository(source).loadThread(address())
            assertEquals(2, loads)
        }

    @Test
    fun sharesTheExactSuccessfulSnapshot() =
        runBlocking {
            var loads = 0
            val snapshot =
                ThreadSnapshot(
                    address(),
                    null,
                    com.solthariel.pawchan.domain.model
                        .ThreadFlags(false, false, false),
                    emptyList(),
                )
            val pass =
                ThreadCheckPassRepository(
                    PassSource {
                        loads++
                        DomainOutcome.Success(snapshot)
                    },
                )
            assertEquals(DomainOutcome.Success(snapshot), pass.loadThread(address()))
            assertEquals(DomainOutcome.Success(snapshot), pass.loadThread(address()))
            assertEquals(1, loads)
        }

    @Test
    fun concurrentConsumersShareOneRequest() =
        runBlocking {
            var loads = 0
            val release = CompletableDeferred<Unit>()
            val pass =
                ThreadCheckPassRepository(
                    PassSource {
                        loads++
                        release.await()
                        DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
                    },
                )
            val first = async { pass.loadThread(address()) }
            val second = async { pass.loadThread(address()) }
            release.complete(Unit)
            assertEquals(first.await(), second.await())
            assertEquals(1, loads)
        }

    @Test
    fun keyIncludesBoardSiteAndThread() =
        runBlocking {
            var loads = 0
            val pass =
                ThreadCheckPassRepository(
                    PassSource {
                        loads++
                        DomainOutcome.Failure(DomainFailure.NotFound)
                    },
                )
            pass.loadThread(address())
            pass.loadThread(address(board = "test"))
            pass.loadThread(address(id = 2))
            pass.loadThread(address(site = "other"))
            assertEquals(4, loads)
        }

    @Test
    fun cancellationIsNotCachedAndUnlocksThePass() =
        runBlocking {
            val started = CompletableDeferred<Unit>()
            var loads = 0
            val pass =
                ThreadCheckPassRepository(
                    PassSource {
                        loads++
                        if (loads == 1) {
                            started.complete(Unit)
                            awaitCancellation()
                        }
                        DomainOutcome.Failure(DomainFailure.NotFound)
                    },
                )
            val first = launch { pass.loadThread(address()) }
            started.await()
            first.cancelAndJoin()
            pass.loadThread(address())
            assertEquals(2, loads)
        }

    private fun address(
        site: String = "2ch",
        board: String = "b",
        id: Long = 1,
    ): ThreadAddress =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse(site)), requireNotNull(BoardId.parse(board))),
            requireNotNull(ThreadId.parse(id)),
        )
}

private class PassSource(
    private val load: suspend () -> DomainOutcome<ThreadSnapshot>,
) : ThreadRepository {
    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> = load()
}
