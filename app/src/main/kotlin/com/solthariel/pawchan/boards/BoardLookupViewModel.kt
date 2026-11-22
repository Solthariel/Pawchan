package com.solthariel.pawchan.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.RecentBoardsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.infrastructure.sites.ForumBoardSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class BoardLookupViewModel(
    private val directory: BoardDirectoryRepository,
    preferences: BoardPreferencesRepository,
    private val recent: RecentBoardsRepository?,
    private val forumSources: List<ForumBoardSource> = emptyList(),
) : ViewModel() {
    private val directoryState = MutableStateFlow(BoardLookupSource())
    private val recentBoards = MutableStateFlow<List<BoardAddress>?>(null)
    private val directoryMutex = Mutex()
    private var loaded = false

    val state =
        combine(directoryState, preferences.favoriteBoards, recentBoards) { source, favorites, visits ->
            source.copy(favorites = favorites, recent = visits.orEmpty(), quickBoardsReady = visits != null)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, BoardLookupSource(quickBoardsReady = false))

    fun visit(current: BoardAddress) {
        viewModelScope.launch {
            val result = recent?.visit(current)
            if (result is DomainOutcome.Success) {
                recentBoards.value = result.value
            } else if (recentBoards.value == null) {
                recentBoards.value = emptyList()
            }
        }
    }

    fun load(force: Boolean = false) {
        viewModelScope.launch { loadDirectory(force) }
    }

    fun selectAdjacentForum(
        current: BoardAddress,
        direction: BoardSwipeDirection,
        onSelected: (BoardAddress) -> Unit,
    ) {
        viewModelScope.launch {
            val target = adjacentForumSource(current.site, forumSources, direction) ?: return@launch
            val boards =
                (
                    if (target.directory === directory) {
                        loadDirectory(force = false)
                    } else {
                        when (val result = target.directory.loadBoards(forceRefresh = false)) {
                            is DomainOutcome.Success -> result.value
                            is DomainOutcome.Failure -> null
                        }
                    }
                ) ?: return@launch
            boardForForum(current, target.site, boards)?.let(onSelected)
        }
    }

    private suspend fun loadDirectory(force: Boolean): List<BoardDirectoryEntry>? =
        directoryMutex.withLock {
            if (loaded && !force) return@withLock directoryState.value.boards
            directoryState.value = directoryState.value.copy(loading = true, failed = false)
            when (val result = directory.loadBoards(forceRefresh = force)) {
                is DomainOutcome.Success -> {
                    loaded = true
                    directoryState.value = directoryState.value.copy(boards = result.value, loading = false)
                    result.value
                }

                is DomainOutcome.Failure -> {
                    directoryState.value = directoryState.value.copy(loading = false, failed = true)
                    null
                }
            }
        }
}
