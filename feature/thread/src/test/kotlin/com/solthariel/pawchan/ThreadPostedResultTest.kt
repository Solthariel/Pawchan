package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.thread.ThreadPersistence
import com.solthariel.pawchan.thread.ThreadUiState
import com.solthariel.pawchan.thread.ThreadViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThreadPostedResultTest {
    @Test
    fun acceptedResultMarksOwnPostBeforeRefreshing() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val address = defaultThreadAddress(42)
                val post = posted(address)
                val snapshot = ThreadSnapshot(address, "thread", ThreadFlags(false, false, false), listOf(post))
                val repository = FakeThreadRepository(DomainOutcome.Success(snapshot), DomainOutcome.Success(snapshot))
                val preferences = TestPostPreferencesRepository()
                val model =
                    ThreadViewModel(
                        address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository(), preferences),
                    )
                advanceUntilIdle()
                model.onPostSubmitted(post)
                advanceUntilIdle()
                assertEquals(2, repository.requests)
                assertTrue(
                    (model.state.value as ThreadUiState.Content)
                        .posts
                        .single()
                        .metadata.changes.own,
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test
    fun resultForAnotherSiteBoardOrThreadDoesNotRefreshThisThread() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val address = defaultThreadAddress(42)
                val snapshot = ThreadSnapshot(address, "thread", ThreadFlags(false, false, false), emptyList())
                val repository = FakeThreadRepository(DomainOutcome.Success(snapshot))
                val model =
                    ThreadViewModel(address, repository, ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()))
                advanceUntilIdle()
                val otherSite = BoardAddress(requireNotNull(SiteId.parse("another")), address.board.board)
                val otherBoard = BoardAddress(address.site, requireNotNull(BoardId.parse("another")))
                model.onPostSubmitted(posted(ThreadAddress(otherSite, address.thread)))
                model.onPostSubmitted(posted(ThreadAddress(otherBoard, address.thread)))
                model.onPostSubmitted(posted(ThreadAddress(address.board, requireNotNull(ThreadId.parse(43)))))
                advanceUntilIdle()
                assertEquals(1, repository.requests)
                assertTrue((model.state.value as ThreadUiState.Content).posts.isEmpty())
            } finally {
                Dispatchers.resetMain()
            }
        }

    private fun posted(address: ThreadAddress): BoardPost =
        BoardPost(
            PostAddress(address, requireNotNull(PostId.parse(44))),
            PostContent(PostDocument.plain("Ответ"), PostMetadata(null, null, false)),
            emptyList(),
        )
}
