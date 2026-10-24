package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.CachedThreadRepository
import com.solthariel.pawchan.domain.watch.ThreadWatchSubscription
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.ThreadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

/** Повторное открытие треда, прочитанного в этой сессии, обходится без сети. */
@OptIn(ExperimentalCoroutinesApi::class)
class ThreadReopenCacheTest {
    private val address = defaultThreadAddress(100)
    private val cached =
        ThreadSnapshot(address, "cached", ThreadFlags(false, false, false), listOf(post(100), post(105)))

    private class Repository(
        private val snapshot: ThreadSnapshot,
        private val current: Boolean,
    ) : CachedThreadRepository {
        var loads = 0

        override suspend fun cachedThread(address: ThreadAddress): ThreadSnapshot = snapshot

        override suspend fun isCachedThreadCurrent(address: ThreadAddress): Boolean = current

        override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
            loads++
            return DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
        }
    }

    private fun reopen(
        repository: Repository,
        watches: FakeWatchRepository = FakeWatchRepository(),
        scope: TestScope,
    ): ThreadViewModel {
        val model = ThreadViewModel(address, repository, ThreadPersistence(FakeThreadLibrary(), watches))
        scope.advanceUntilIdle()
        return model
    }

    @Test fun threadReadInThisSessionReopensWithoutNetwork() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = Repository(cached, current = true)
                val model = reopen(repository, scope = this)
                assertEquals(0, repository.loads)
                assertEquals("cached", (model.state.value as ThreadUiState.Content).subject)
                model.refresh()
                advanceUntilIdle()
                assertEquals(1, repository.loads)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun watcherCheckpointNewerThanCacheLoadsNewPosts() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val watches = FakeWatchRepository()
                watches.subscription = ThreadWatchSubscription(address, null, PostId.parse(110), 0L)
                val repository = Repository(cached, current = true)
                reopen(repository, watches, this)
                assertEquals(1, repository.loads)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun diskOnlyCopyIsShownAndRefreshedFromNetwork() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = Repository(cached, current = false)
                val model = reopen(repository, scope = this)
                assertEquals(1, repository.loads)
                assertEquals("cached", (model.state.value as ThreadUiState.Content).subject)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun retainedSessionReenteredKeepsPostsWithoutNetwork() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = Repository(cached, current = true)
                val model = reopen(repository, scope = this)
                val shown = model.state.value
                model.reopen()
                advanceUntilIdle()
                assertEquals(0, repository.loads)
                assertSame(shown, model.state.value)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun retainedSessionWithNewPostsRefreshesOverShownPosts() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val watches = FakeWatchRepository()
                val repository = Repository(cached, current = true)
                val model = reopen(repository, watches, this)
                assertEquals(0, repository.loads)
                watches.subscription = ThreadWatchSubscription(address, null, PostId.parse(110), 0L)
                model.reopen()
                advanceUntilIdle()
                assertEquals(1, repository.loads)
                // Ошибка сети при фоновом обновлении не убирает уже показанные посты.
                assertEquals("cached", (model.state.value as ThreadUiState.Content).subject)
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun post(id: Long): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(id))),
            PostContent(PostDocument(listOf(PostTextRun("text"))), PostMetadata(null, null, false)),
            emptyList(),
        )
}
