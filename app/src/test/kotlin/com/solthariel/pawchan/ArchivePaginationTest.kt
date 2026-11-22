package com.solthariel.pawchan

import com.solthariel.pawchan.arhivach.ArchiveBrowseState
import com.solthariel.pawchan.arhivach.ArchiveLoadState
import com.solthariel.pawchan.arhivach.ArchiveNextPageRequest
import com.solthariel.pawchan.arhivach.ArchiveOpenThreadState
import com.solthariel.pawchan.arhivach.ArchivePaginationState
import com.solthariel.pawchan.arhivach.canLoadNextPage
import com.solthariel.pawchan.arhivach.shouldAutoLoad
import com.solthariel.pawchan.domain.archive.ArchiveTag
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchivePaginationTest {
    private val ready = ArchiveBrowseState(list = ArchiveThreadPage(emptyList(), hasNext = true))

    @Test fun nextPageRequiresAvailableIdleList() {
        assertTrue(ready.canLoadNextPage())
        assertFalse(ArchiveBrowseState().canLoadNextPage())
        assertFalse(ready.copy(list = ArchiveThreadPage(emptyList(), hasNext = false)).canLoadNextPage())
        assertFalse(ready.copy(pagination = ArchivePaginationState(loadingNextPage = true)).canLoadNextPage())
        assertFalse(ready.copy(load = ArchiveLoadState(loading = true)).canLoadNextPage())
        assertFalse(ready.copy(openThread = ArchiveOpenThreadState(id = 55)).canLoadNextPage())
    }

    @Test fun stalePageRequestCannotUpdateDifferentFeed() {
        val state = ready.copy(tags = listOf(ArchiveTag(2, "/b/")), pagination = ArchivePaginationState(page = 1))
        val request = ArchiveNextPageRequest(startPage = 0, page = 2, tags = listOf(2))
        assertTrue(request.matches(state))
        assertFalse(request.matches(state.copy(tags = emptyList())))
        assertFalse(request.matches(state.copy(pagination = ArchivePaginationState(startPage = 1, page = 1))))
        assertFalse(request.matches(state.copy(pagination = ArchivePaginationState(page = 2))))
        assertFalse(request.matches(state.copy(openThread = ArchiveOpenThreadState(id = 55))))
    }

    @Test fun automaticLoadingStopsForFilterErrorOrInFlightPage() {
        assertTrue(ready.shouldAutoLoad(shownCount = 0, autoLoadPages = true))
        assertFalse(ready.shouldAutoLoad(shownCount = 0, autoLoadPages = false))
        assertFalse(ready.shouldAutoLoad(shownCount = 1, autoLoadPages = true))
        assertFalse(ready.copy(pagination = ArchivePaginationState(loadingNextPage = true)).shouldAutoLoad(0, true))
        assertFalse(ready.copy(pagination = ArchivePaginationState(nextPageError = "Ошибка")).shouldAutoLoad(0, true))
        assertFalse(ready.copy(list = ArchiveThreadPage(emptyList(), hasNext = false)).shouldAutoLoad(0, true))
    }
}
