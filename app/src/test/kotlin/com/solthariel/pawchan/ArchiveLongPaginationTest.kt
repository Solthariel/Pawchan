package com.solthariel.pawchan

import com.solthariel.pawchan.arhivach.ArchiveBrowseModel
import com.solthariel.pawchan.domain.archive.ArchiveBrowser
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import com.solthariel.pawchan.domain.archive.ArchiveThreadSummary
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
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveLongPaginationTest {
    @Test fun longFeedDeduplicatesOverlapsAndSurvivesOpeningThread() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val browser = LongFeedBrowser()
                val model = ArchiveBrowseModel(browser, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                repeat(99) {
                    model.loadNextPage()
                    advanceUntilIdle()
                }
                val before = model.state.value
                assertEquals(99, before.page)
                assertEquals((0L..4504L).toList(), before.threadCards.map { it.summary.id })
                assertFalse(requireNotNull(before.list).hasNext)
                model.loadNextPage()
                advanceUntilIdle()
                assertEquals(100, browser.requests)
                model.openThread(4000)
                advanceUntilIdle()
                assertEquals(
                    4000L,
                    model.state.value.thread
                        ?.id,
                )
                model.backToList()
                assertNull(model.state.value.thread)
                assertEquals(before.threadCards, model.state.value.threadCards)
                assertEquals(before.page, model.state.value.page)
                model.selectTag(ArchiveTag(7, "тест"))
                advanceUntilIdle()
                assertEquals(0, model.state.value.page)
                assertEquals(
                    (0L..49L).toList(),
                    model.state.value.threadCards
                        .map { it.summary.id },
                )
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class LongFeedBrowser : ArchiveBrowser {
    var requests = 0

    override suspend fun searchTags(query: String): DomainOutcome<List<ArchiveTag>> = DomainOutcome.Success(emptyList())

    override suspend fun loadThreads(
        tags: List<Long>,
        page: Int,
    ): DomainOutcome<ArchiveThreadPage> {
        requests++
        val threads =
            (page * 45L until page * 45L + 50L).map { id ->
                ArchiveThreadSummary(id, "Тред $id", 1, null, emptyList())
            }
        return DomainOutcome.Success(ArchiveThreadPage(threads, hasNext = page < 99, totalPages = 100))
    }

    override suspend fun loadThread(id: Long): DomainOutcome<ArchiveThread> =
        DomainOutcome.Success(ArchiveThread(id, "Тред $id", emptyList()))
}
