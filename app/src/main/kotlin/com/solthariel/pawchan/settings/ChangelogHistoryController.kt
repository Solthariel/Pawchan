package com.solthariel.pawchan.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

internal data class ChangelogHistoryUiState(
    val releases: List<ChangelogRelease>? = null,
    val failed: Boolean = false,
)

/** Settings owns one live history; update discovery and dialog opening share it. */
internal class ChangelogHistoryController(
    private val repository: ChangelogHistoryRepository,
    private val scope: CoroutineScope,
) {
    private val mutableState = MutableStateFlow(ChangelogHistoryUiState())
    private var minimumCode = 0L
    private var entry: Any? = null
    private var entryRevision = 0L
    private var refreshedEntry: Long? = null
    private val refreshMutex = Mutex()
    val state = mutableState.asStateFlow()

    fun enter(request: Any) {
        if (entry === request) return
        entry = request
        entryRevision++
        scope.launch { open() }
    }

    fun updateFound(code: Long) {
        if (code <= minimumCode) return
        minimumCode = code
        scope.launch { refresh() }
    }

    suspend fun open() {
        try {
            publish(repository.local())
            refresh()
        } catch (_: IOException) {
            failIfEmpty()
        } catch (_: IllegalArgumentException) {
            failIfEmpty()
        }
    }

    private suspend fun refresh() {
        refreshMutex.withLock {
            val requestedEntry = entryRevision
            val latestCode =
                state.value.releases
                    ?.firstOrNull()
                    ?.code
                    ?.toLong() ?: 0L
            if (refreshedEntry == requestedEntry && latestCode >= minimumCode) return@withLock
            repository.refresh(minimumCode)?.let {
                publish(it)
                refreshedEntry = requestedEntry
            }
        }
    }

    private fun publish(releases: List<ChangelogRelease>) {
        mutableState.value = ChangelogHistoryUiState(mergeHistory(releases, state.value.releases.orEmpty()))
    }

    private fun failIfEmpty() {
        mutableState.value = state.value.copy(failed = state.value.releases == null)
    }
}
