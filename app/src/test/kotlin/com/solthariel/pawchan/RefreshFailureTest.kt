package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogError
import com.solthariel.pawchan.catalog.CatalogUiState
import com.solthariel.pawchan.catalog.keepContentOnRefreshFailure
import com.solthariel.pawchan.core.ui.RefreshFailure
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class RefreshFailureTest {
    @Test
    fun catalogRefreshRetainsRowsAndStopsSpinnerOn429() {
        val previous = CatalogUiState.Content("Бред", 100, emptyList(), isRefreshing = true)
        val next =
            CatalogUiState.Error(CatalogError.RATE_LIMITED).keepContentOnRefreshFailure(previous)
                as CatalogUiState.Content
        assertSame(previous.threads, next.threads)
        assertEquals(false, next.isRefreshing)
        assertEquals(RefreshFailure(rateLimited = true), next.refreshFailure)
    }

    @Test
    fun initialLoadAndNotFoundAreNotMaskedByOldContent() {
        val error = CatalogUiState.Error(CatalogError.SERVER)
        assertSame(error, error.keepContentOnRefreshFailure(CatalogUiState.Loading))
        val missing = CatalogUiState.Error(CatalogError.NOT_FOUND)
        assertSame(missing, missing.keepContentOnRefreshFailure(CatalogUiState.Content("Бред", 0, emptyList())))
    }
}
