package com.solthariel.pawchan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadSession
import com.solthariel.pawchan.thread.ThreadUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadSessionFactoryTest {
    @Test
    fun publicFactoryLoadsWithoutPublishingTheConcreteViewModel() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val repository = FakeThreadRepository(snapshot(address))
                val provider = ViewModelProvider(owner, factory(address, repository))
                val session = provider[ThreadSession::class.java]
                val state = session.state.first { it is ThreadUiState.Content } as ThreadUiState.Content
                assertEquals(address, state.address)
                assertEquals(1, repository.requests)
                assertSame(session, provider[ThreadSession::class.java])
            } finally {
                owner.clear()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun distinctStoreKeysHaveIndependentSessionsAndAddressedRepositories() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val first = defaultThreadAddress(42)
                val second = requireNotNull(threadAddress("another", "b", 42))
                val firstRepository = FakeThreadRepository(snapshot(first))
                val secondRepository = FakeThreadRepository(snapshot(second))
                val a = ViewModelProvider(owner, factory(first, firstRepository))["first", ThreadSession::class.java]
                val b = ViewModelProvider(owner, factory(second, secondRepository))["second", ThreadSession::class.java]
                assertTrue(a !== b)
                assertEquals(first, (a.state.first { it is ThreadUiState.Content } as ThreadUiState.Content).address)
                assertEquals(second, (b.state.first { it is ThreadUiState.Content } as ThreadUiState.Content).address)
            } finally {
                owner.clear()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun clearingStoreCancelsThePublicSessionRequest() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            var cancelled = false
            val repository =
                object : ThreadRepository {
                    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled = true
                        }
                    }
                }
            try {
                ViewModelProvider(owner, factory(defaultThreadAddress(42), repository))[ThreadSession::class.java]
                runCurrent()
                assertTrue(!cancelled)
                owner.clear()
                runCurrent()
                assertTrue(cancelled)
            } finally {
                owner.clear()
                runCurrent()
                Dispatchers.resetMain()
            }
        }

    private fun factory(
        address: ThreadAddress,
        repository: ThreadRepository,
    ) = ThreadSession.factory(address, repository, ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()))

    private fun snapshot(address: ThreadAddress) =
        DomainOutcome.Success(ThreadSnapshot(address, "Тред", ThreadFlags(false, false, false), emptyList()))
}
