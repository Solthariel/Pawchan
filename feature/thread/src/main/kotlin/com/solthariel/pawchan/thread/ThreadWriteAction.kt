package com.solthariel.pawchan.thread

/** Loading, errors and closed threads cannot open a new reply from navigation. */
internal fun threadWriteAction(
    state: ThreadUiState,
    onReply: (ThreadReplyRequest) -> Unit,
): (() -> Unit)? = threadWriteAction(state, onReply, true)

internal fun threadWriteAction(
    state: ThreadUiState,
    onReply: (ThreadReplyRequest) -> Unit,
    repliesEnabled: Boolean,
): (() -> Unit)? =
    if (repliesEnabled && state is ThreadUiState.Content && !state.closed) {
        { onReply(ThreadReplyRequest(closed = false, insertion = null)) }
    } else {
        null
    }
