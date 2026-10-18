package com.solthariel.pawchan.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.boards.BoardRepositories
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.navigation.CatalogNavigation
import com.solthariel.pawchan.navigation.saveableKey

@Composable
internal fun rememberCatalogMenu(
    board: BoardAddress,
    repositories: BoardRepositories,
    navigation: CatalogNavigation,
): CatalogMenuBindings {
    val factory = remember(board, repositories) { CatalogMenuViewModel.factory(board, repositories) }
    val model: CatalogMenuViewModel = viewModel(key = "board-menu:${board.saveableKey}", factory = factory)
    val preferences by model.preferences.collectAsStateWithLifecycle()
    val information by model.information.collectAsStateWithLifecycle()
    return CatalogMenuBindings(
        CatalogMenuState(preferences, information),
        BoardPreferenceActions(model::toggleFavorite, model::makeHome, model::setTheme),
        BoardMenuNavigation(
            { navigation.onArchive(board) },
            { navigation.onNewThreadClick(board) },
            model::showInformation,
        ),
        model::dismiss,
        CatalogHiddenThreadsMenu(),
    )
}
