package com.solthariel.pawchan.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.boards.ARCHIVE_SOURCE_ID
import com.solthariel.pawchan.boards.BoardSwipeDirection
import com.solthariel.pawchan.boards.BoardsAddressBindings
import com.solthariel.pawchan.boards.BoardsAddressTarget
import com.solthariel.pawchan.boards.BoardsListUi
import com.solthariel.pawchan.boards.BoardsNavigation
import com.solthariel.pawchan.boards.BoardsPresentationCache
import com.solthariel.pawchan.boards.BoardsScreen
import com.solthariel.pawchan.boards.BoardsViewModel
import com.solthariel.pawchan.boards.CATEGORY_CHIPS_HEIGHT
import com.solthariel.pawchan.boards.SOURCE_PICKER_HEIGHT
import com.solthariel.pawchan.boards.adjacentBoardSourceTab
import com.solthariel.pawchan.boards.forumSwipeGesture
import com.solthariel.pawchan.catalog.BoardsActions
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.FloatingTopChromeState
import com.solthariel.pawchan.core.ui.rememberFloatingTopChromeState
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
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.workspace.LocalWorkspace
import com.solthariel.pawchan.workspace.openThread
import kotlinx.coroutines.flow.map

@Composable
internal fun BoardsRoute(
    dependencies: AppDependencies,
    onBoardSelected: (BoardAddress) -> Unit,
    bottomActions: AppBottomActions,
    currentBoard: () -> BoardAddress? = { null },
    owner: androidx.lifecycle.ViewModelStoreOwner,
    boardLists: MutableMap<String, BoardsListUi>,
) {
    val sources = dependencies.repositories.boards.sources
    val chrome = rememberFloatingTopChromeState(APP_TOP_BAR_HEIGHT + SOURCE_PICKER_HEIGHT + CATEGORY_CHIPS_HEIGHT)
    val presentationCache = remember(chrome, boardLists) { BoardsPresentationCache(boardLists, chrome) }
    val screenStates = rememberSaveableStateHolder()
    val sharedSelection = LocalSelectedForum.current
    var localSelection by rememberSaveable {
        mutableStateOf(currentBoard()?.site?.value ?: sources.firstOrNull()?.site?.value)
    }
    val selected = sharedSelection?.value ?: localSelection
    val archiveSelected = selected == ARCHIVE_SOURCE_ID
    val source = sources.firstOrNull { it.site.value == selected } ?: sources.firstOrNull()
    val site = source?.site ?: defaultBoardAddress().site
    val directory = source?.directory ?: dependencies.boardDirectoryRepository
    val model = rememberBoardsModel(directory, dependencies, site.value, owner)
    val address = rememberBoardsAddress(site, currentBoard, onBoardSelected)
    val selectSource: (String) -> Unit = { target ->
        localSelection = target
        sharedSelection?.value = target
    }
    val onForumSwipe: (BoardSwipeDirection) -> Unit = { direction ->
        selected?.let { current ->
            adjacentBoardSourceTab(current, sources, direction)?.let(selectSource)
        }
    }
    AppNavigationWorkspace(
        actions = if (archiveSelected) bottomActions.copy(onWriteClick = null) else bottomActions,
        selected = AppBottomDestination.BOARDS,
        floatingTop = true,
    ) {
        val picker: @Composable (Modifier) -> Unit = { modifier ->
            com.solthariel.pawchan.boards.BoardSourcePicker(
                sources,
                site,
                modifier,
                archiveSelected,
            ) {
                selectSource(it.value)
            }
        }
        if (archiveSelected) {
            ArchiveDirectoryPlaceholder(picker, bottomActions.onCatalogClick, onForumSwipe, chrome)
        } else {
            key(site.value) {
                screenStates.SaveableStateProvider(site.value) {
                    val state by model.state.collectAsStateWithLifecycle()
                    BoardsScreen(
                        state = state,
                        navigation = BoardsNavigation(onBoardSelected, bottomActions.onCatalogClick, onForumSwipe),
                        actions = BoardsActions(model::setFavorite, model::setCategoryExpanded, model::refresh),
                        address = address,
                        presentationCache = presentationCache,
                        sourcePicker = picker,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchiveDirectoryPlaceholder(
    picker: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onForumSwipe: (BoardSwipeDirection) -> Unit,
    chrome: FloatingTopChromeState,
) {
    com.solthariel.pawchan.core.ui.FloatingTopChrome(state = chrome, toolbar = {
        com.solthariel.pawchan.settings.SettingsPageToolbar(
            title = "Архивач",
            back = onBack,
            actions = {},
        )
        com.solthariel.pawchan.boards
            .BoardsSourceFilterRow(sourcePicker = picker)
    }) {
        Box(Modifier.fillMaxSize().forumSwipeGesture(onForumSwipe))
    }
}

@Composable
internal fun rememberBoardsModel(
    directory: BoardDirectoryRepository,
    dependencies: AppDependencies,
    site: String,
    owner: androidx.lifecycle.ViewModelStoreOwner,
): BoardsViewModel {
    val factory =
        remember(directory, dependencies) {
            BoardsViewModel.factory(
                directory,
                dependencies.boardPreferencesRepository,
                dependencies.postingAccessRepository,
            )
        }
    return viewModel(viewModelStoreOwner = owner, key = "boards:$site", factory = factory)
}

@Composable
private fun rememberBoardsAddress(
    site: com.solthariel.pawchan.domain.model.SiteId,
    currentBoard: () -> BoardAddress?,
    onBoardSelected: (BoardAddress) -> Unit,
): BoardsAddressBindings {
    val mirror = LocalForumMirror.current
    val workspace = LocalWorkspace.current
    val links = remember(mirror) { AppPostLinkResolver(appForumWebLinks(mirror)) }
    return BoardsAddressBindings(
        site = site,
        resolve = { input ->
            val context =
                currentBoard()?.takeIf { it.site == site }
                    ?: BoardAddress(site, defaultBoardAddress().board)
            links.addressInput(input, context)?.toBoardsTarget()
        },
        open = { target ->
            when (target) {
                is BoardsAddressTarget.Board -> onBoardSelected(target.board)
                is BoardsAddressTarget.Thread -> workspace?.openThread(target.address, target.postId)
            }
        },
    )
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
