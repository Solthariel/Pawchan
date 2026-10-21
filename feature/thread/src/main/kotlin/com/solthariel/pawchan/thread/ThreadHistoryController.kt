package com.solthariel.pawchan.thread

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadHistoryRepository
import com.solthariel.pawchan.domain.thread.withRestoredPosts
import com.solthariel.pawchan.domain.thread.withoutDeleted
import com.solthariel.pawchan.domain.thread.withoutOldDeleted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Thread-scope history edits share the network mutex; the session still owns presentation and refresh. */
internal class ThreadHistoryController(
    private val scope: CoroutineScope,
    private val address: ThreadAddress,
    private val history: ThreadHistoryRepository,
    private val requestMutex: Mutex,
    private val content: () -> ThreadUiState.Content?,
    private val showSnapshot: suspend (ThreadSnapshot, Boolean) -> Unit,
) {
    fun actions(refresh: () -> Unit): ThreadHistoryActions =
        ThreadHistoryActions(
            restore = { archived, done -> edit(done) { it.withRestoredPosts(archived) } },
            clearOld = { edit { it.withoutOldDeleted() } },
            clearDeleted = { edit { it.withoutDeleted() } },
            erase = { erase(refresh) },
        )

    private fun edit(
        done: (Int) -> Unit = {},
        transform: (ThreadSnapshot) -> ThreadSnapshot,
    ) {
        scope.launch {
            requestMutex.withLock {
                val current = content() ?: return@withLock
                val stored = history.load(address) ?: return@withLock
                currentCoroutineContext().ensureActive()
                val edited = transform(stored)
                if (edited != stored) {
                    history.save(edited)
                    currentCoroutineContext().ensureActive()
                    showSnapshot(edited, current.isFavorite)
                }
                done(edited.posts.size - stored.posts.size)
            }
        }
    }

    private fun erase(refresh: () -> Unit) {
        scope.launch {
            requestMutex.withLock { history.erase(address) }
            currentCoroutineContext().ensureActive()
            refresh()
        }
    }
}
