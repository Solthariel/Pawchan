package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.boards.BoardLookupContent
import com.solthariel.pawchan.boards.BoardLookupPanel
import com.solthariel.pawchan.boards.BoardLookupSource
import com.solthariel.pawchan.boards.BoardLookupViewModel
import com.solthariel.pawchan.boards.prepareLookup
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.domain.model.BoardAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun rememberCatalogBoardLookup(
    board: BoardAddress,
    dependencies: AppDependencies,
    onSelected: (BoardAddress) -> Unit,
): CatalogBoardLookupBindings {
    var visible by rememberSaveable(board.saveableKey) { mutableStateOf(false) }
    var query by rememberSaveable(board.saveableKey) { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val model = rememberBoardLookupModel(board, dependencies)
    LaunchedEffect(model) { model.visit(board) }
    val dismiss = {
        visible = false
        query = ""
        focus.clearFocus()
        keyboard?.hide()
        Unit
    }
    return CatalogBoardLookupBindings(
        visible,
        query,
        { query = it },
        {
            query = ""
            visible = true
            model.load()
        },
        dismiss,
    ) {
        val source by model.state.collectAsStateWithLifecycle()
        val prepared by rememberLookupContent(source, board, query)
        BoardLookupPanel(
            prepared,
            source,
            { chosen ->
                if (prepared.query == query) {
                    dismiss()
                    if (chosen != board) onSelected(chosen)
                }
            },
            { model.load(force = true) },
        )
    }
}

@Composable
private fun rememberBoardLookupModel(
    board: BoardAddress,
    dependencies: AppDependencies,
): BoardLookupViewModel {
    val factory =
        remember(board, dependencies) {
            viewModelFactory {
                initializer {
                    BoardLookupViewModel(
                        dependencies.boardDirectoryRepository,
                        dependencies.boardPreferencesRepository,
                        dependencies.repositories.boards.recent,
                    )
                }
            }
        }
    return viewModel(key = "board-lookup:${board.saveableKey}", factory = factory)
}

@Composable
private fun rememberLookupContent(
    source: BoardLookupSource,
    board: BoardAddress,
    query: String,
) = produceState(BoardLookupContent("", emptyList(), emptyList(), emptyList()), source, board, query) {
    value = withContext(Dispatchers.Default) { source.prepareLookup(board, query) }
}
