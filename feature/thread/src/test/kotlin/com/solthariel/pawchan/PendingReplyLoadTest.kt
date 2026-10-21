package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.CachedThreadRepository
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingReplyLoadTest {
    @Test fun missingTargetRefreshesCurrentCacheButExistingPostDoesNot() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = PendingReplyRepository()
                val model =
                    ThreadViewModel(
                        repository.address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()),
                    )
                advanceUntilIdle()
                assertEquals(0, repository.loads)
                model.ensurePost(105)
                assertEquals(0, repository.loads)
                model.ensurePost(110)
                runCurrent()
                assertEquals(1, repository.loads)
                repository.response.complete(repository.snapshot)
                advanceUntilIdle()
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun existingLoadIsNotCancelledOrDuplicatedWhenTargetArrives() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val repository = PendingReplyRepository()
                val model =
                    ThreadViewModel(
                        repository.address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()),
                    )
                advanceUntilIdle()
                model.refresh()
                runCurrent()
                val waiting = launch { model.ensurePost(105) }
                runCurrent()
                assertEquals(1, repository.loads)
                assertEquals(false, waiting.isCompleted)
                repository.response.complete(repository.snapshot)
                advanceUntilIdle()
                assertEquals(true, waiting.isCompleted)
                assertEquals(1, repository.loads)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class PendingReplyRepository : CachedThreadRepository {
    val address = defaultThreadAddress(100)
    val snapshot = ThreadSnapshot(address, "Cached", ThreadFlags(false, false, false), listOf(post(100), post(105)))
    val response = CompletableDeferred<ThreadSnapshot>()
    var loads = 0

    private fun post(id: Long): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(id))),
            PostContent(PostDocument.plain("Post $id"), PostMetadata(null, null, false)),
            emptyList(),
        )

    override suspend fun cachedThread(address: ThreadAddress): ThreadSnapshot = snapshot

    override suspend fun isCachedThreadCurrent(address: ThreadAddress): Boolean = true

    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> {
        loads++
        return DomainOutcome.Success(response.await())
    }
}
