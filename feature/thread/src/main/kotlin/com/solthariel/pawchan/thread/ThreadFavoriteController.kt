package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Scope/state are Main-confined; the mutex serializes repository reads and writes. */
internal class ThreadFavoriteController(
    private val scope: CoroutineScope,
    private val address: ThreadAddress,
    private val repository: ThreadLibraryRepository,
    private val state: PostContentAccess,
    private val nowMillis: () -> Long,
) {
    private val mutex = Mutex()
    private var latest: Boolean? = null

    fun set(favorite: Boolean) {
        scope.launch {
            mutex.withLock {
                val content = state.read() as? ThreadUiState.Content ?: return@withLock
                val previous = content.isFavorite
                if (previous == favorite) return@withLock
                publish(favorite)
                val result = repository.setFavorite(address, content.subject, favorite, nowMillis())
                if (result is DomainOutcome.Failure) publish(previous)
            }
        }
    }

    fun refresh() {
        scope.launch {
            mutex.withLock { readFavorite()?.let(::publish) }
        }
    }

    suspend fun load(outcome: DomainOutcome<ThreadSnapshot>): Boolean =
        if (outcome is DomainOutcome.Success) {
            mutex.withLock { readFavorite()?.also { latest = it } ?: latest ?: false }
        } else {
            false
        }

    fun decorate(content: ThreadUiState): ThreadUiState {
        val favorite = latest ?: return content
        return if (content is ThreadUiState.Content) {
            content.copy(presentation = content.presentation.copy(isFavorite = favorite))
        } else {
            content
        }
    }

    private fun publish(favorite: Boolean) {
        latest = favorite
        state.write(decorate(state.read()))
    }

    private suspend fun readFavorite(): Boolean? =
        when (val result = repository.loadEntry(address)) {
            is DomainOutcome.Success<ThreadLibraryEntry?> -> result.value?.favorite == true
            is DomainOutcome.Failure -> null
        }
}
