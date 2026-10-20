package com.solthariel.pawchan.thread

import com.solthariel.pawchan.core.ui.RefreshFailure

/** Preserve readable content when a refresh fails. */
internal fun ThreadUiState.keepContentOnRefreshFailure(previous: ThreadUiState): ThreadUiState {
    if (this !is ThreadUiState.Error || previous !is ThreadUiState.Content) {
        return this
    }
    val missing = reason == ThreadError.NOT_FOUND
    val failure = if (missing) null else RefreshFailure(reason == ThreadError.RATE_LIMITED, challengeUrl)
    val notice = previous.presentation.missingThreadNotice + if (missing) 1 else 0
    return previous.copy(
        presentation =
            previous.presentation.copy(
                isRefreshing = false,
                refreshFailure = failure,
                missingThreadNotice = notice,
            ),
    )
}
