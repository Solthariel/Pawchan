package com.solthariel.pawchan

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.ThreadViewModel
import com.solthariel.pawchan.thread.ThreadWorkspaceEvents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
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
class ThreadWorkspaceBoundaryTest {
    @Test
    fun acceptedPostReachesWorkspaceBeforeTheRefreshRequest() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val history = mutableListOf<String>()
                val events = RecordingWorkspaceEvents(history)
                val repository =
                    CallbackThreadRepository {
                        history += "load"
                        DomainOutcome.Success(snapshot(address))
                    }
                val model = model(owner, address, repository, events)
                advanceUntilIdle()
                history.clear()
                model.onPostSubmitted(post(address))
                advanceUntilIdle()
                assertEquals(listOf("own:44", "load", "observe:42"), history)
                assertEquals(
                    address,
                    events.posts
                        .single()
                        .address.thread,
                )
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun anotherThreadResultDoesNotDeliverWorkspaceEvents() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val address = defaultThreadAddress(42)
                val history = mutableListOf<String>()
                val events = RecordingWorkspaceEvents(history)
                val repository = FakeThreadRepository(DomainOutcome.Success(snapshot(address)))
                val model = model(owner, address, repository, events)
                advanceUntilIdle()
                history.clear()
                model.onPostSubmitted(post(defaultThreadAddress(43)))
                advanceUntilIdle()
                assertTrue(events.posts.isEmpty())
                assertTrue(history.isEmpty())
                assertEquals(1, repository.requests)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun failedLoadDoesNotPublishAnInventedWorkspaceSnapshot() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                val history = mutableListOf<String>()
                val events = RecordingWorkspaceEvents(history)
                val repository = FakeThreadRepository(DomainOutcome.Failure(DomainFailure.NetworkUnavailable))
                val model = model(owner, defaultThreadAddress(42), repository, events)
                advanceUntilIdle()
                assertTrue(history.isEmpty())
                assertTrue(model.state.value is ThreadUiState.Error)
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    @Test
    fun ownerClearCancelsRequestWithoutDeliveringLateWorkspaceObservation() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            val owner = ViewModelStore()
            try {
                var cancelled = false
                val history = mutableListOf<String>()
                val repository =
                    CallbackThreadRepository {
                        try {
                            awaitCancellation()
                        } finally {
                            cancelled = true
                        }
                    }
                model(owner, defaultThreadAddress(42), repository, RecordingWorkspaceEvents(history))
                runCurrent()
                owner.clear()
                advanceUntilIdle()
                assertTrue(cancelled)
                assertTrue(history.isEmpty())
            } finally {
                owner.clear()
                Dispatchers.resetMain()
            }
        }

    private fun model(
        owner: ViewModelStore,
        address: ThreadAddress,
        repository: ThreadRepository,
        events: ThreadWorkspaceEvents,
    ): ThreadViewModel {
        val factory =
            viewModelFactory {
                initializer {
                    ThreadViewModel(
                        address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository(), workspaceEvents = events),
                    )
                }
            }
        return ViewModelProvider(owner, factory)[ThreadViewModel::class.java]
    }

    private fun snapshot(address: ThreadAddress): ThreadSnapshot =
        ThreadSnapshot(address, "thread", ThreadFlags(false, false, false), emptyList())

    private fun post(address: ThreadAddress): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(44))),
            PostContent(PostDocument.plain("post"), PostMetadata(null, null, false)),
            emptyList(),
        )
}

private class RecordingWorkspaceEvents(
    private val history: MutableList<String>,
) : ThreadWorkspaceEvents {
    val posts = mutableListOf<BoardPost>()

    override fun observe(snapshot: ThreadSnapshot) {
        history += "observe:${snapshot.address.thread.value}"
    }

    override fun storeOwn(post: BoardPost) {
        posts += post
        history += "own:${post.address.post.value}"
    }
}

private class CallbackThreadRepository(
    private val load: suspend () -> DomainOutcome<ThreadSnapshot>,
) : ThreadRepository {
    override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> = load()
}
