package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.core.ui.RefreshFailure

/** A failed refresh is not an initial load: keep readable content and its lazy keys. */
internal fun CatalogUiState.keepContentOnRefreshFailure(previous: CatalogUiState): CatalogUiState {
    if (this !is CatalogUiState.Error || previous !is CatalogUiState.Content || reason == CatalogError.NOT_FOUND) {
        return this
    }
    return previous.copy(
        isRefreshing = false,
        refreshFailure = RefreshFailure(reason == CatalogError.RATE_LIMITED, challengeUrl),
    )
}
