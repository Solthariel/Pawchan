package com.solthariel.pawchan.arhivach

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.LoadErrorPane
import com.solthariel.pawchan.core.ui.TransientNotice

internal fun ArchiveBrowseState.hasActiveContent(): Boolean =
    if (selectedThread == null) {
        list != null
    } else {
        thread != null
    }

@Composable
internal fun ArchiveBrowseFeedback(
    state: ArchiveBrowseState,
    onRetry: () -> Unit,
    content: @Composable () -> Unit,
) {
    val error = state.load.error
    if (error != null && !state.hasActiveContent()) {
        LoadErrorPane(message = error, onRetry = onRetry)
    } else {
        content()
        TransientNotice(error)
    }
}
