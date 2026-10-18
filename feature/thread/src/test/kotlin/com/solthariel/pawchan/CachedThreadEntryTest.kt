package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.CachedThreadRepository
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.ThreadViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedThreadEntryTest {
    @Test fun cachedEntryDoesNotWaitForNetwork() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val address = defaultThreadAddress(100)
                val waiting = CompletableDeferred<DomainOutcome<ThreadSnapshot>>()
                val snapshot = ThreadSnapshot(address, "cached", ThreadFlags(false, false, false), emptyList())
                val repository =
                    object : CachedThreadRepository {
                        override suspend fun cachedThread(address: ThreadAddress): ThreadSnapshot = snapshot

                        override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                            require(address == snapshot.address)
                            return waiting.await()
                        }
                    }
                val model =
                    ThreadViewModel(
                        address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()),
                    )
                runCurrent()
                assertEquals("cached", (model.state.value as ThreadUiState.Content).subject)
                assertTrue(!waiting.isCompleted)
                waiting.complete(
                    DomainOutcome.Failure(com.solthariel.pawchan.domain.model.DomainFailure.NetworkUnavailable),
                )
                advanceUntilIdle()
                assertEquals("cached", (model.state.value as ThreadUiState.Content).subject)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun addingFavoriteTwiceDoesNotRemoveIt() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val address = defaultThreadAddress(100)
                val snapshot = ThreadSnapshot(address, "thread", ThreadFlags(false, false, false), emptyList())
                val library = FakeThreadLibrary()
                val model =
                    ThreadViewModel(
                        address,
                        FakeThreadRepository(DomainOutcome.Success(snapshot)),
                        ThreadPersistence(library, FakeWatchRepository()),
                    )
                advanceUntilIdle()
                model.setFavorite(true)
                advanceUntilIdle()
                model.setFavorite(true)
                advanceUntilIdle()
                assertTrue(library.favorite)
                assertTrue((model.state.value as ThreadUiState.Content).isFavorite)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
