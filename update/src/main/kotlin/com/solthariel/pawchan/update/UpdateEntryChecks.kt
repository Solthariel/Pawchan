package com.solthariel.pawchan.update

/** One caller-owned visit, not a recomposition or a lazy row's lifetime. */
public class UpdateCheckRequest

internal class UpdateEntryChecks {
    private var consumed: UpdateCheckRequest? = null

    fun consume(
        request: UpdateCheckRequest?,
        state: UpdateUiState,
    ): Boolean {
        if (request == null || consumed === request) return false
        consumed = request
        return state !is UpdateUiState.Downloading && state !is UpdateUiState.Ready
    }
}
