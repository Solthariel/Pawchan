package com.solthariel.pawchan.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.boards.BoardRepositories
import com.solthariel.pawchan.domain.boards.BoardDetailsRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.SettingsRepository
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.navigation.defaultBoardAddress
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal class CatalogMenuViewModel(
    private val board: BoardAddress,
    private val favorites: BoardPreferencesRepository,
    private val settings: SettingsRepository,
    private val details: BoardDetailsRepository,
) : ViewModel() {
    private val saving = MutableStateFlow(BoardMenuPreferences())
    private var informationJob: Job? = null
    private val mutableInformation = MutableStateFlow<BoardInformationState>(BoardInformationState.Closed)
    val information = mutableInformation.asStateFlow()
    val preferences =
        combine(favorites.favoriteBoards, settings.homeBoard, settings.theme, saving) { boards, home, theme, write ->
            write.copy(favorite = board.board in boards, home = board == (home ?: defaultBoardAddress()), theme = theme)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, BoardMenuPreferences(saving = true))

    fun toggleFavorite() {
        val favorite = !preferences.value.favorite
        save { favorites.setFavoriteBoard(board.board, favorite) }
    }

    fun makeHome() {
        save { settings.setHomeBoard(board) }
    }

    fun setTheme(theme: ThemePreference) {
        save { settings.setTheme(theme) }
    }

    private fun save(action: suspend () -> DomainOutcome<Unit>) {
        if (saving.value.saving || preferences.value.saving) return
        saving.value = BoardMenuPreferences(saving = true)
        viewModelScope.launch {
            try {
                saving.value = BoardMenuPreferences(saveFailed = action() is DomainOutcome.Failure)
            } finally {
                saving.value = saving.value.copy(saving = false)
            }
        }
    }

    fun showInformation() {
        informationJob?.cancel()
        mutableInformation.value = BoardInformationState.Loading
        informationJob =
            viewModelScope.launch {
                mutableInformation.value =
                    when (val result = details.loadInformation(board)) {
                        is DomainOutcome.Success -> BoardInformationState.Content(result.value)
                        is DomainOutcome.Failure -> BoardInformationState.Error(result.reason)
                    }
            }
    }

    fun dismiss() {
        informationJob?.cancel()
        mutableInformation.value = BoardInformationState.Closed
        saving.value = saving.value.copy(saveFailed = false)
    }

    companion object {
        fun factory(
            board: BoardAddress,
            repositories: BoardRepositories,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    CatalogMenuViewModel(board, repositories.preferences, repositories.settings, repositories.details)
                }
            }
    }
}
