package com.solthariel.pawchan.catalog

import com.solthariel.pawchan.domain.boards.BoardInformation
import com.solthariel.pawchan.domain.model.DomainFailure
import com.solthariel.pawchan.domain.settings.ThemePreference

internal data class BoardMenuPreferences(
    val favorite: Boolean = false,
    val home: Boolean = false,
    val theme: ThemePreference = ThemePreference.SYSTEM,
    val saving: Boolean = false,
    val saveFailed: Boolean = false,
)

internal sealed interface BoardInformationState {
    data object Closed : BoardInformationState

    data object Loading : BoardInformationState

    data class Content(
        val information: BoardInformation,
    ) : BoardInformationState

    data class Error(
        val failure: DomainFailure,
    ) : BoardInformationState
}

internal data class CatalogMenuState(
    val preferences: BoardMenuPreferences,
    val information: BoardInformationState,
)

internal data class BoardPreferenceActions(
    val onFavorite: () -> Unit,
    val onHome: () -> Unit,
    val onTheme: (ThemePreference) -> Unit,
)

internal data class BoardMenuNavigation(
    val onArchive: () -> Unit,
    val onNewThread: () -> Unit,
    val onInformation: () -> Unit,
)

internal data class CatalogMenuBindings(
    val state: CatalogMenuState,
    val preferences: BoardPreferenceActions,
    val navigation: BoardMenuNavigation,
    val onDismiss: () -> Unit,
    val hiddenThreads: CatalogHiddenThreadsMenu? = null,
    val boardLookup: CatalogBoardLookupBindings? = null,
)

internal data class CatalogHiddenThreadsMenu(
    val reveal: (() -> Unit)? = null,
    val revealed: Boolean = false,
    val count: Int = 0,
)
