package com.solthariel.pawchan

import com.solthariel.pawchan.arhivach.ArchiveBrowseModel
import com.solthariel.pawchan.domain.archive.ArchiveBrowser
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import com.solthariel.pawchan.domain.archive.ArchiveThreadSummary
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ArchiveRefreshRetentionTest {
    @Test fun failedListRefreshKeepsAllLoadedPagesAndSuccessfulRetryReplacesThem() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val browser = RetentionBrowser()
                val model = ArchiveBrowseModel(browser, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                model.loadNextPage()
                advanceUntilIdle()
                browser.failList = true
                model.loadNextPage()
                advanceUntilIdle()
                val before = model.state.value
                assertEquals(listOf(55L, 56L), before.threadCards.map { it.summary.id })
                assertNotNull(before.nextPageError)
                model.refresh()
                assertTrue(model.state.value.load.loading)
                assertEquals(before.threadCards, model.state.value.threadCards)
                assertEquals(before.pagination, model.state.value.pagination)
                advanceUntilIdle()
                val failed = model.state.value
                assertEquals(before.list, failed.list)
                assertEquals(before.threadCards, failed.threadCards)
                assertEquals(before.pagination, failed.pagination)
                assertEquals("Ошибка при загрузке данных", failed.load.error)
                assertFalse(failed.load.loading)
                browser.failList = false
                model.refresh()
                advanceUntilIdle()
                assertNull(model.state.value.load.error)
                assertEquals(
                    listOf(55L),
                    model.state.value.threadCards
                        .map { it.summary.id },
                )
                assertEquals(0, model.state.value.page)
                assertNull(model.state.value.nextPageError)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun failedThreadRefreshKeepsThreadAndDifferentThreadDoesNotReuseIt() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val browser = RetentionBrowser()
                val model = ArchiveBrowseModel(browser, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                model.openThread(55)
                advanceUntilIdle()
                val thread = model.state.value.thread
                assertNotNull(thread)
                browser.failThread = true
                model.refresh()
                assertEquals(thread, model.state.value.thread)
                advanceUntilIdle()
                assertEquals(thread, model.state.value.thread)
                assertEquals("Ошибка при загрузке данных", model.state.value.load.error)
                model.openThread(56)
                assertNull(model.state.value.thread)
                advanceUntilIdle()
                assertNull(model.state.value.thread)
                assertEquals("Ошибка при загрузке данных", model.state.value.load.error)
                browser.failThread = false
                model.refresh()
                advanceUntilIdle()
                assertEquals(
                    56L,
                    model.state.value.thread
                        ?.id,
                )
                assertNull(model.state.value.load.error)
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun firstLoadAndNavigationFailureNeverReuseWrongFeed() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val browser = RetentionBrowser().apply { failList = true }
                val model = ArchiveBrowseModel(browser, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                assertNull(model.state.value.list)
                assertEquals("Ошибка при загрузке данных", model.state.value.load.error)
                browser.failList = false
                model.refresh()
                advanceUntilIdle()
                assertNotNull(model.state.value.list)
                browser.failList = true
                model.loadPage(1)
                advanceUntilIdle()
                assertNull(model.state.value.list)
                assertTrue(
                    model.state.value.threadCards
                        .isEmpty(),
                )
                assertEquals(1, model.state.value.startPage)
                browser.failList = false
                model.loadPage(0)
                advanceUntilIdle()
                browser.failList = true
                model.selectTag(ArchiveTag(2, "/b/"))
                advanceUntilIdle()
                assertNull(model.state.value.list)
                assertTrue(
                    model.state.value.threadCards
                        .isEmpty(),
                )
                assertEquals(
                    listOf(2L),
                    model.state.value.tags
                        .map { it.id },
                )
            } finally {
                Dispatchers.resetMain()
            }
        }

    @Test fun cancelledOldFailureCannotOverwriteNewPage() =
        runTest {
            val dispatcher = StandardTestDispatcher(testScheduler)
            Dispatchers.setMain(dispatcher)
            try {
                val browser = RetentionBrowser()
                val model = ArchiveBrowseModel(browser, backgroundDispatcher = dispatcher)
                advanceUntilIdle()
                val gate = CompletableDeferred<Unit>()
                browser.delayedFailure = gate
                model.refresh()
                runCurrent()
                model.loadPage(1)
                advanceUntilIdle()
                assertEquals(
                    listOf(56L),
                    model.state.value.threadCards
                        .map { it.summary.id },
                )
                gate.complete(Unit)
                advanceUntilIdle()
                assertEquals(
                    listOf(56L),
                    model.state.value.threadCards
                        .map { it.summary.id },
                )
                assertNull(model.state.value.load.error)
            } finally {
                Dispatchers.resetMain()
            }
        }
}

private class RetentionBrowser : ArchiveBrowser {
    var failList = false
    var failThread = false
    var delayedFailure: CompletableDeferred<Unit>? = null

    override suspend fun searchTags(query: String): DomainOutcome<List<ArchiveTag>> = DomainOutcome.Success(emptyList())

    override suspend fun loadThreads(
        tags: List<Long>,
        page: Int,
    ): DomainOutcome<ArchiveThreadPage> {
        val gate = delayedFailure
        if (gate != null) {
            delayedFailure = null
            withContext(NonCancellable) { gate.await() }
            return DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
        }
        return if (failList) {
            DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
        } else {
            val summary = ArchiveThreadSummary(55L + page, "Тема $page", 1, null, emptyList())
            DomainOutcome.Success(ArchiveThreadPage(listOf(summary), hasNext = page < 2, totalPages = 3))
        }
    }

    override suspend fun loadThread(id: Long): DomainOutcome<ArchiveThread> =
        if (failThread) {
            DomainOutcome.Failure(DomainFailure.NetworkUnavailable)
        } else {
            DomainOutcome.Success(ArchiveThread(id, "Тема", listOf(ArchivePost("1", null, null, "Текст", emptyList()))))
        }
}
