package com.solthariel.pawchan.boards

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.RecentBoardsRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class BoardLookupViewModel(
    private val directory: BoardDirectoryRepository,
    preferences: BoardPreferencesRepository,
    private val recent: RecentBoardsRepository?,
) : ViewModel() {
    private val directoryState = MutableStateFlow(BoardLookupSource())
    private val recentBoards = MutableStateFlow(emptyList<BoardAddress>())
    private var loaded = false

    val state =
        combine(directoryState, preferences.favoriteBoards, recentBoards) { source, favorites, visits ->
            source.copy(favorites = favorites, recent = visits)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, BoardLookupSource())

    fun visit(current: BoardAddress) {
        viewModelScope.launch {
            val result = recent?.visit(current)
            if (result is DomainOutcome.Success) recentBoards.value = result.value
        }
    }

    fun load(force: Boolean = false) {
        if (directoryState.value.loading || (loaded && !force)) return
        directoryState.value = directoryState.value.copy(loading = true, failed = false)
        viewModelScope.launch {
            val result = directory.loadBoards(forceRefresh = force)
            directoryState.value =
                when (result) {
                    is DomainOutcome.Success -> {
                        loaded = true
                        directoryState.value.copy(boards = result.value, loading = false)
                    }

                    is DomainOutcome.Failure -> {
                        directoryState.value.copy(loading = false, failed = true)
                    }
                }
        }
    }
}
