package com.solthariel.pawchan.library

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryRefreshStateTest {
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42)),
        )
    private val content = LibraryUiState.Content(listOf(LibraryEntryUi(address, "Тема", 1, true)))

    @Test fun failedRefreshKeepsEntriesAndSignalsError() {
        assertEquals(content.copy(refreshFailures = 1), libraryRefreshState(content, LibraryUiState.Error))
    }

    @Test fun failedInitialOrEmptyLoadShowsError() {
        assertEquals(LibraryUiState.Error, libraryRefreshState(LibraryUiState.Loading, LibraryUiState.Error))
        val empty = LibraryUiState.Content(emptyList())
        assertEquals(LibraryUiState.Error, libraryRefreshState(empty, LibraryUiState.Error))
    }

    @Test fun repeatedFailuresRemainDistinct() {
        val failed = libraryRefreshState(content, LibraryUiState.Error)
        assertEquals(content.copy(refreshFailures = 2), libraryRefreshState(failed, LibraryUiState.Error))
    }

    @Test fun successfulRefreshReplacesStaleDataAndClearsError() {
        val refreshed = LibraryUiState.Content(emptyList())
        assertEquals(refreshed, libraryRefreshState(content.copy(refreshFailures = 1), refreshed))
    }
}
