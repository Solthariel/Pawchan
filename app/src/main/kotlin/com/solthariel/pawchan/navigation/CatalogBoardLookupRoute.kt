package com.solthariel.pawchan.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.solthariel.pawchan.boards.BoardLookupPanel
import com.solthariel.pawchan.boards.BoardLookupViewModel
import com.solthariel.pawchan.boards.BoardRepositories
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.SiteId

@Composable
internal fun rememberCatalogBoardLookup(
    board: BoardAddress,
    dependencies: AppDependencies,
    onSelected: (BoardAddress) -> Unit,
    tags: List<String>,
    onTagSelected: (String?) -> Unit,
): CatalogBoardLookupBindings {
    var visible by rememberSaveable(board.saveableKey) { mutableStateOf(false) }
    var query by rememberSaveable(board.saveableKey) { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val sharedSelection = LocalSelectedForum.current
    val site = resolveLookupSite(dependencies, sharedSelection?.value, board.site)
    val model = rememberBoardLookupModel(board, site, dependencies)
    val boardSiteModel = rememberBoardLookupModel(board, board.site, dependencies)
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
        { direction ->
            boardSiteModel.selectAdjacentForum(board, direction) { adjacent ->
                sharedSelection?.value = adjacent.site.value
                onSelected(adjacent)
            }
        },
    ) {
        val source by model.state.collectAsStateWithLifecycle()
        val prepared by rememberLookupContent(source, board, query, site)
        prepared?.let { content ->
            BoardLookupPanel(
                content.copy(interactive = content.query == query),
                source,
                { chosen ->
                    if (content.query == query) {
                        dismiss()
                        onTagSelected(null)
                        if (chosen != board) onSelected(chosen)
                    }
                },
                { model.load(force = true) },
                tags = dependencies.repositories.boards.lookupTags(board.site, site, tags),
                selectTag = { tag ->
                    if (content.query == query) {
                        dismiss()
                        onTagSelected(tag)
                    }
                },
            )
        }
    }
}

@Composable
private fun rememberBoardLookupModel(
    board: BoardAddress,
    site: SiteId,
    dependencies: AppDependencies,
): BoardLookupViewModel {
    val factory =
        remember(board, site, dependencies) {
            viewModelFactory {
                initializer {
                    val directory =
                        dependencies.repositories.boards.sources
                            .firstOrNull { it.site == site }
                            ?.directory
                            ?: dependencies.boardDirectoryRepository
                    BoardLookupViewModel(
                        directory,
                        dependencies.boardPreferencesRepository,
                        dependencies.repositories.boards.recent,
                        forumSources = dependencies.repositories.boards.sources,
                    )
                }
            }
        }
    return viewModel(key = "board-lookup:${site.value}:${board.saveableKey}", factory = factory)
}

private fun BoardRepositories.lookupTags(
    boardSite: SiteId,
    lookupSite: SiteId,
    tags: List<String>,
): List<String> = if (lookupSite == boardSite && capabilities(lookupSite).tags) tags else emptyList()

private fun resolveLookupSite(
    dependencies: AppDependencies,
    requested: String?,
    fallback: SiteId,
): SiteId =
    dependencies.repositories.boards.sources
        .firstOrNull { it.site.value == requested }
        ?.site ?: fallback
