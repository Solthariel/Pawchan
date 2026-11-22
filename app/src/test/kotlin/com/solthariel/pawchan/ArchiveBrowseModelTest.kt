package com.solthariel.pawchan

import com.solthariel.pawchan.arhivach.ArchiveBrowseModel
import com.solthariel.pawchan.domain.archive.ArchiveBrowser
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.archive.ArchiveThreadMetadata
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import com.solthariel.pawchan.domain.archive.ArchiveThreadSummary
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveBrowseModelTest {
    @Test fun tagFilteringPagingThreadAndBack() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val api = FakeArchiveBrowser()
                val model = ArchiveBrowseModel(api, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                assertEquals(listOf<Long>(), api.lastTags)
                assertEquals(0, api.lastPage)
                val firstCard =
                    model.state.value
                        .threadCards
                        .single()
                assertEquals(55L, firstCard.summary.id)
                assertEquals(1, firstCard.summary.postCount)
                assertTrue(firstCard.tags.isEmpty())
                model.queryChanged("бред")
                advanceUntilIdle()
                val tag =
                    model.state.value.search.suggestions
                        .single()
                model.selectTag(tag)
                advanceUntilIdle()
                assertEquals(listOf(2L), api.lastTags)
                assertEquals("", model.state.value.search.query)
                model.loadPage(1)
                advanceUntilIdle()
                assertEquals(1, api.lastPage)
                model.openThread(55)
                advanceUntilIdle()
                assertEquals(
                    "Текст",
                    model.state.value.thread
                        ?.posts
                        ?.single()
                        ?.text,
                )
                model.backToList()
                assertNull(model.state.value.thread)
                assertEquals(1, model.state.value.page)
                assertTrue(model.state.value.list != null)
                model.removeTag(tag)
                advanceUntilIdle()
                assertEquals(emptyList<Long>(), api.lastTags)
                assertEquals(0, api.lastPage)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun archiveStatusFollowsTheSelectedThreadAndSurvivesRefresh() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val api = FakeArchiveBrowser().apply { statuses = mapOf(55L to "Удален?", 56L to "Сохранен") }
                val model = ArchiveBrowseModel(api, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                model.loadNextPage()
                advanceUntilIdle()

                model.openThread(55)
                advanceUntilIdle()
                assertEquals("Удален?", model.state.value.openThread.status)
                model.refresh()
                advanceUntilIdle()
                assertEquals("Удален?", model.state.value.openThread.status)

                model.openThread(56)
                advanceUntilIdle()
                assertEquals("Сохранен", model.state.value.openThread.status)
                model.backToList()
                assertNull(model.state.value.openThread.status)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun unknownArchiveStatusDoesNotInheritThePreviousThreadStatus() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val api = FakeArchiveBrowser().apply { statuses = mapOf(55L to "Удален?") }
                val model = ArchiveBrowseModel(api, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                model.openThread(55)
                advanceUntilIdle()
                assertEquals("Удален?", model.state.value.openThread.status)
                model.openThread(999)
                advanceUntilIdle()
                assertNull(model.state.value.openThread.status)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun failuresAreVisibleAndNeverBecomeEmptySuccesses() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val api = FakeArchiveBrowser().apply { fail = true }
                val model = ArchiveBrowseModel(api, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                assertEquals("Ошибка при загрузке данных", model.state.value.load.error)
                assertNull(model.state.value.list)
                assertFalse(model.state.value.load.loading)
                model.queryChanged("ошибка")
                advanceUntilIdle()
                assertTrue(model.state.value.search.error)
                model.openThread(55)
                advanceUntilIdle()
                assertEquals("Ошибка при загрузке данных", model.state.value.load.error)
                assertNull(model.state.value.thread)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun scrollingLoadsAndAppendsPagesUntilTheArchiveEnds() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val api = FakeArchiveBrowser()
                val model = ArchiveBrowseModel(api, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                assertEquals(
                    listOf(55L),
                    model.state.value.threadCards
                        .map { it.summary.id },
                )

                model.loadNextPage()
                model.loadNextPage()
                advanceUntilIdle()
                assertEquals(1, api.lastPage)
                assertEquals(1, api.requestedPages.count { it == 1 })
                assertEquals(
                    listOf(55L, 56L),
                    model.state.value.threadCards
                        .map { it.summary.id },
                )
                assertEquals(1, model.state.value.page)

                api.failPage = 2
                model.loadNextPage()
                advanceUntilIdle()
                assertEquals(
                    listOf(55L, 56L),
                    model.state.value.threadCards
                        .map { it.summary.id },
                )
                assertEquals("Ошибка при загрузке данных", model.state.value.nextPageError)
                assertFalse(model.state.value.loadingNextPage)

                api.failPage = null
                model.loadNextPage()
                advanceUntilIdle()
                assertEquals(
                    listOf(55L, 56L, 57L),
                    model.state.value.threadCards
                        .map { it.summary.id },
                )
                assertEquals(2, model.state.value.page)
                assertFalse(
                    model.state.value.list
                        ?.hasNext ?: true,
                )
                assertNull(model.state.value.nextPageError)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class FakeArchiveBrowser : ArchiveBrowser {
    var fail = false
    var failPage: Int? = null
    var statuses: Map<Long, String> = emptyMap()
    var lastTags = emptyList<Long>()
    var lastPage = 0
    val requestedPages = mutableListOf<Int>()

    override suspend fun searchTags(query: String): DomainOutcome<List<ArchiveTag>> =
        if (fail) failure() else DomainOutcome.Success(listOf(ArchiveTag(2, "/b/")))

    override suspend fun loadThreads(
        tags: List<Long>,
        page: Int,
    ): DomainOutcome<ArchiveThreadPage> {
        lastTags = tags
        lastPage = page
        requestedPages += page
        return if (fail || failPage == page) {
            failure()
        } else {
            DomainOutcome.Success(
                ArchiveThreadPage(
                    listOf(
                        ArchiveThreadSummary(
                            55L + page,
                            "Тема $page",
                            1,
                            null,
                            emptyList(),
                            metadata = ArchiveThreadMetadata(status = statuses[55L + page]),
                        ),
                    ),
                    hasNext = page < 2,
                    totalPages = 3,
                ),
            )
        }
    }

    override suspend fun loadThread(id: Long): DomainOutcome<ArchiveThread> =
        if (fail) {
            failure()
        } else {
            DomainOutcome.Success(
                ArchiveThread(id, "Тема", listOf(ArchivePost("999", "Аноним", null, "Текст", emptyList()))),
            )
        }

    private fun failure(): DomainOutcome.Failure = DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
}
