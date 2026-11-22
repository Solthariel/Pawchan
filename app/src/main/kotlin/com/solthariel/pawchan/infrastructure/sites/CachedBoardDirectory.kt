package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardDirectorySnapshots
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.model.SiteId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** One independent cache per site; never changes the delegate's networking or proxy route. */
internal class CachedBoardDirectory(
    private val site: SiteId,
    private val delegate: BoardDirectoryRepository,
    file: File,
) : BoardDirectoryRepository by delegate,
    BoardDirectorySnapshots {
    private val store = BoardDirectoryCacheFile(site, file)
    private val mutex = Mutex()
    private var restored = false
    private var fetchedThisSession = false
    private val mutableSnapshots = MutableStateFlow<List<BoardDirectoryEntry>?>(null)
    override val snapshots = mutableSnapshots.asStateFlow()

    suspend fun restore() = mutex.withLock { restoreLocked() }

    suspend fun refreshAtStartup() =
        mutex.withLock {
            restoreLocked()
            if (!fetchedThisSession) fetchLocked(true)
        }

    override suspend fun loadBoards(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> =
        mutex.withLock {
            restoreLocked()
            val cached = mutableSnapshots.value
            if (!forceRefresh && cached != null) DomainOutcome.Success(cached) else fetchLocked(forceRefresh)
        }

    private suspend fun restoreLocked() {
        if (!restored) {
            mutableSnapshots.value = withContext(Dispatchers.IO) { store.read() }
            restored = true
        }
    }

    private suspend fun fetchLocked(forceRefresh: Boolean): DomainOutcome<List<BoardDirectoryEntry>> {
        val result = delegate.loadBoards(forceRefresh)
        if (result is DomainOutcome.Success) {
            if (result.value.isEmpty() || result.value.any { it.address.site != site }) {
                return DomainOutcome.Failure(DomainFailure.InvalidResponse)
            }
            fetchedThisSession = true
            val boards = result.value.toList()
            mutableSnapshots.value = boards
            withContext(Dispatchers.IO) { store.write(boards) }
        }
        return result
    }
}
