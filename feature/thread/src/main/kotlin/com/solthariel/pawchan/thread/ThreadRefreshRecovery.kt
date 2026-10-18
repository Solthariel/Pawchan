package com.solthariel.pawchan.thread

import com.solthariel.pawchan.core.ui.RefreshFailure

/** Preserve readable content when a refresh fails. */
internal fun ThreadUiState.keepContentOnRefreshFailure(previous: ThreadUiState): ThreadUiState {
    if (this !is ThreadUiState.Error || previous !is ThreadUiState.Content || reason == ThreadError.NOT_FOUND) {
        return this
    }
    return previous.copy(
        presentation =
            previous.presentation.copy(
                isRefreshing = false,
                refreshFailure = RefreshFailure(reason == ThreadError.RATE_LIMITED, challengeUrl),
            ),
    )
}
