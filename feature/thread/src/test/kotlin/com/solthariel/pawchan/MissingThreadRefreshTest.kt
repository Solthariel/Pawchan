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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MissingThreadRefreshTest {
    @Test
    fun repeatedMissingRefreshesRetainCachedContentAndPublishNotices() =
        runTest {
            Dispatchers.setMain(StandardTestDispatcher(testScheduler))
            try {
                val address = defaultThreadAddress(100)
                val post =
                    BoardPost(
                        PostAddress(address, requireNotNull(PostId.parse(100))),
                        PostContent(
                            PostDocument(listOf(PostTextRun("Сохранённый пост"))),
                            PostMetadata(null, null, false),
                        ),
                        emptyList(),
                    )
                val snapshot = ThreadSnapshot(address, "cached", ThreadFlags(false, false, false), listOf(post))
                val repository =
                    object : CachedThreadRepository {
                        override suspend fun cachedThread(address: ThreadAddress): ThreadSnapshot = snapshot

                        override suspend fun loadThread(address: ThreadAddress): DomainOutcome<ThreadSnapshot> =
                            DomainOutcome.Failure(DomainFailure.NotFound)
                    }
                val model =
                    ThreadViewModel(
                        address,
                        repository,
                        ThreadPersistence(FakeThreadLibrary(), FakeWatchRepository()),
                    )
                advanceUntilIdle()
                val first = model.state.value as ThreadUiState.Content
                assertEquals("cached", first.subject)
                assertEquals(1L, first.presentation.missingThreadNotice)
                model.refresh()
                advanceUntilIdle()
                val second = model.state.value as ThreadUiState.Content
                assertEquals(first.posts, second.posts)
                assertEquals(
                    "Сохранённый пост",
                    second.posts
                        .single()
                        .document.plainText,
                )
                assertEquals(2L, second.presentation.missingThreadNotice)
                assertEquals(false, second.isRefreshing)
            } finally {
                Dispatchers.resetMain()
            }
        }
}
