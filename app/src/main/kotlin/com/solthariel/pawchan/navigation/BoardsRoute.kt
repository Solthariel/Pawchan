package com.solthariel.pawchan.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.boards.BoardsAddressBindings
import com.solthariel.pawchan.boards.BoardsAddressTarget
import com.solthariel.pawchan.boards.BoardsScreen
import com.solthariel.pawchan.boards.BoardsViewModel
import com.solthariel.pawchan.catalog.BoardsActions
import com.solthariel.pawchan.catalog.CatalogBottomActions
import com.solthariel.pawchan.catalog.CatalogBottomDestination
import com.solthariel.pawchan.catalog.CatalogNavigationWorkspace
import com.solthariel.pawchan.domain.boards.BoardDirectoryRepository
import com.solthariel.pawchan.domain.boards.BoardPreferencesRepository
import com.solthariel.pawchan.domain.boards.PostingAccessRepository
import com.solthariel.pawchan.domain.catalog.CatalogRepository
import com.solthariel.pawchan.domain.library.ThreadLibraryRepository
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.posting.PostingRepository
import com.solthariel.pawchan.domain.postpreferences.PostPreferencesRepository
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.thread.ThreadRepository
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import com.solthariel.pawchan.infrastructure.sites.LocalForumMirror
import com.solthariel.pawchan.infrastructure.sites.appForumWebLinks
import com.solthariel.pawchan.infrastructure.thread.AppPostLinkResolver
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.openThread
import kotlinx.coroutines.flow.map

@Composable
internal fun BoardsRoute(
    dependencies: AppDependencies,
    onBoardSelected: (BoardAddress) -> Unit,
    bottomActions: CatalogBottomActions,
    currentBoard: () -> BoardAddress? = { null },
) {
    val mirror = LocalForumMirror.current
    val workspace = LocalWorkspace.current
    val links =
        remember(mirror) {
            AppPostLinkResolver(appForumWebLinks(mirror))
        }
    val address =
        BoardsAddressBindings(
            resolve = { input -> links.addressInput(input, currentBoard())?.toBoardsTarget() },
            open = { target ->
                when (target) {
                    is BoardsAddressTarget.Board -> {
                        onBoardSelected(target.board)
                    }

                    is BoardsAddressTarget.Thread -> {
                        workspace?.openThread(target.address, target.postId)
                    }
                }
            },
        )
    val factory =
        remember(
            dependencies.boardDirectoryRepository,
            dependencies.boardPreferencesRepository,
            dependencies.postingAccessRepository,
        ) {
            BoardsViewModel.factory(
                dependencies.boardDirectoryRepository,
                dependencies.boardPreferencesRepository,
                dependencies.postingAccessRepository,
            )
        }
    val viewModel: BoardsViewModel = viewModel(factory = factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    CatalogNavigationWorkspace(
        actions = bottomActions,
        selected = CatalogBottomDestination.BOARDS,
    ) {
        BoardsScreen(
            state = state,
            onBoardSelected = { board ->
                onBoardSelected(BoardAddress(site = defaultBoardAddress().site, board = board))
            },
            onBack = bottomActions.onCatalogClick,
            actions =
                BoardsActions(
                    onFavoriteChanged = viewModel::setFavorite,
                    onCategoryExpanded = viewModel::setCategoryExpanded,
                    onRetry = viewModel::refresh,
                ),
            address = address,
        )
    }
}

private fun ForumWebLink.toBoardsTarget(): BoardsAddressTarget? =
    when (this) {
        is ForumWebLink.Board -> {
            BoardsAddressTarget.Board(board)
        }

        is ForumWebLink.Thread -> {
            BoardsAddressTarget.Thread(thread, postId)
        }

        is ForumWebLink.Media -> {
            null
        }
    }
