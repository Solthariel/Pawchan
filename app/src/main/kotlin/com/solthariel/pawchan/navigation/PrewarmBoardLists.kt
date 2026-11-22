package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.ViewModelStoreOwner
import com.solthariel.pawchan.boards.BoardsListUi
import com.solthariel.pawchan.boards.BoardsUiState
import com.solthariel.pawchan.boards.boardListDimensions
import com.solthariel.pawchan.boards.prepareBoardsList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

/** Prepare each source without entering its screen; keep work tied to the navigation host. */
@Composable
internal fun PrewarmBoardLists(
    dependencies: AppDependencies,
    owner: ViewModelStoreOwner,
    lists: MutableMap<String, BoardsListUi>,
) {
    val dimensions = boardListDimensions()
    val density = LocalDensity.current.density
    dependencies.repositories.boards.sources.forEach { source ->
        val model = rememberBoardsModel(source.directory, dependencies, source.site.value, owner)
        LaunchedEffect(model, dimensions, density) {
            model.state.collectLatest { state ->
                if (state is BoardsUiState.Content) {
                    val prepared =
                        withContext(Dispatchers.Default) {
                            state.prepareBoardsList("", dimensions, density, categoryFilter = "")
                        }
                    lists["${source.site.value}\u0000false\u0000"] = prepared
                }
            }
        }
    }
}
