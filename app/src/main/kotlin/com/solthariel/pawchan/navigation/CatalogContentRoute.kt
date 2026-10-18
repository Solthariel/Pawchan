package com.solthariel.pawchan.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.solthariel.pawchan.catalog.BindCatalogAgeUpdates
import com.solthariel.pawchan.catalog.CatalogActions
import com.solthariel.pawchan.catalog.CatalogBottomActions
import com.solthariel.pawchan.catalog.CatalogContentActions
import com.solthariel.pawchan.catalog.CatalogNavigationActions
import com.solthariel.pawchan.catalog.CatalogPresentation
import com.solthariel.pawchan.catalog.CatalogScreen
import com.solthariel.pawchan.catalog.CatalogUiState
import com.solthariel.pawchan.catalog.CatalogViewModel
import com.solthariel.pawchan.catalog.rememberCatalogMenu
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.infrastructure.media.MediaContextMenuHost
import com.solthariel.pawchan.infrastructure.media.MediaMenuTargets
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaContentHost
import com.solthariel.pawchan.media.MediaMenuPlace
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaSaveActions
import com.solthariel.pawchan.media.postMediaSelection
import com.solthariel.pawchan.media.rememberMediaSaveActions
import com.solthariel.pawchan.workspace.LocalWorkspace
import kotlinx.coroutines.flow.map

@Composable
internal fun CatalogContentRoute(
    board: BoardAddress,
    dependencies: AppDependencies,
    navigation: CatalogNavigation,
    challengeRetryToken: Long,
    bottomActions: CatalogBottomActions,
    onMediaVisibilityChanged: (Boolean) -> Unit = {},
) {
    val filters =
        com.solthariel.pawchan.settings
            .rememberQuickContentFilter(dependencies.appSettings)
    var openedAttachment by remember(board) { mutableStateOf<MediaAttachmentUi?>(null) }
    var mediaMenu by remember(board) { mutableStateOf<MediaMenuRequest?>(null) }
    LaunchedEffect(openedAttachment) { onMediaVisibilityChanged(openedAttachment != null) }
    val viewModel = rememberCatalogViewModel(board, dependencies)
    val state by viewModel.state.collectAsStateWithLifecycle()
    BindCatalogAgeUpdates(viewModel)
    BindCatalogRouteEffects(challengeRetryToken, viewModel)
    val mediaSave = rememberMediaSaveActions(dependencies.mediaLoaders.mediaDownloadQueue)
    val closeAttachment = { openedAttachment = null }
    BackHandler(enabled = openedAttachment != null, onBack = closeAttachment)
    MediaContentHost(
        selection =
            postMediaSelection(openedAttachment, (state as? CatalogUiState.Content)?.viewerMediaGroups.orEmpty()) {
                openedAttachment = it
            },
        mediaLoaders = dependencies.mediaLoaders.viewer,
        mediaSave = mediaSave,
        onMenu = { mediaMenu = it },
        onClose = closeAttachment,
    ) {
        val catalogActions =
            catalogRouteActions(
                board = board,
                navigation = navigation,
                viewModel = viewModel,
                mediaSave = mediaSave,
                onOpenAttachment = { openedAttachment = it },
                onMediaMenu = { mediaMenu = MediaMenuRequest(it, MediaMenuPlace.THUMBNAIL) },
            ).copy(
                threadMenuHost = rememberCatalogThreadMenuHost(dependencies.library),
                onToggleFiltered = viewModel::toggleFilteredThreads,
                onAddRule =
                    filters?.let { tools ->
                        { thread -> tools.add(catalogRuleSelection(thread)) }
                    },
            )
        CatalogScreen(
            presentation =
                CatalogPresentation(
                    board,
                    state,
                    dependencies.mediaLoaders.imageLoader,
                    dependencies.mediaLoaders.thumbnailLoader,
                ),
            actions = catalogActions,
            bottomActions = bottomActions,
            menu = rememberCatalogMenu(board, dependencies.repositories.boards, navigation),
        )
    }
    val samples = (state as? CatalogUiState.Content)?.filterSamples.orEmpty()
    filters?.editor?.invoke(samples)
    CatalogMediaContextMenu(mediaMenu, dependencies, mediaSave, state, navigation) { mediaMenu = null }
}

@Composable
private fun CatalogMediaContextMenu(
    request: MediaMenuRequest?,
    dependencies: AppDependencies,
    save: com.solthariel.pawchan.media.MediaSaveActions,
    state: CatalogUiState,
    navigation: CatalogNavigation,
    close: () -> Unit,
) {
    MediaContextMenuHost(
        request = request,
        loaders = dependencies.mediaLoaders,
        save = save,
        onNotice = {},
        targets = MediaMenuTargets(openThread = { openAttachmentThread(state, it, navigation) }),
        onClose = close,
    )
}

private fun catalogRuleSelection(
    thread: com.solthariel.pawchan.catalog.CatalogThreadUi,
): com.solthariel.pawchan.thread.ContentFilterSelection =
    com.solthariel.pawchan.thread
        .ContentFilterSelection(thread.address.board, thread.content.filterText, false)

@Composable
private fun rememberCatalogViewModel(
    board: BoardAddress,
    dependencies: AppDependencies,
): CatalogViewModel {
    val workspace = LocalWorkspace.current?.controller
    val factory =
        remember(board, dependencies.catalogRepository, workspace) {
            CatalogViewModel.factory(
                dependencies.catalogRepository,
                board,
                workspace?.state?.map { it.marks.hidden } ?: kotlinx.coroutines.flow.flowOf(emptySet()),
                dependencies.appSettings?.appSettings?.map { it.contentFilters }
                    ?: kotlinx.coroutines.flow.flowOf(emptyList()),
                dependencies.appSettings?.appSettings?.map { it.behavior.showHiddenThreads }
                    ?: kotlinx.coroutines.flow.flowOf(false),
                dependencies.filterStatistics,
            )
        }
    return viewModel(key = board.saveableKey, factory = factory)
}

/** «Открыть тред» из меню миниатюры: тред, которому принадлежит файл. */
private fun openAttachmentThread(
    state: CatalogUiState,
    attachment: MediaAttachmentUi,
    navigation: CatalogNavigation,
) {
    (state as? CatalogUiState.Content)
        ?.threads
        ?.firstOrNull { thread -> thread.attachments.any { it.originalUrl == attachment.originalUrl } }
        ?.let { navigation.onThreadClick(it.address) }
}

@Composable
private fun BindCatalogRouteEffects(
    challengeRetryToken: Long,
    catalog: CatalogViewModel,
) {
    LaunchedEffect(challengeRetryToken) {
        if (challengeRetryToken > 0L) {
            catalog.refresh()
        }
    }
}

private fun catalogRouteActions(
    board: BoardAddress,
    navigation: CatalogNavigation,
    viewModel: CatalogViewModel,
    mediaSave: MediaSaveActions,
    onOpenAttachment: (MediaAttachmentUi) -> Unit,
    onMediaMenu: (MediaAttachmentUi) -> Unit,
): CatalogActions =
    CatalogActions(
        content =
            CatalogContentActions(
                onThreadClick = navigation.onThreadClick,
                onMediaClick = { selected ->
                    if (selected.viewer == AttachmentViewer.NONE) {
                        mediaSave.request(selected)
                    } else {
                        onOpenAttachment(selected)
                    }
                },
                onMediaLongClick = onMediaMenu,
                onRefresh = viewModel::refresh,
                onSearchQueryChange = viewModel::setSearchQuery,
                onChallenge = navigation.onChallenge,
            ),
        navigation =
            CatalogNavigationActions(
                onLibraryClick = navigation.onLibraryClick,
                onSettingsClick = navigation.onSettingsClick,
                onNewThreadClick = navigation.onNewThreadClick,
                onBoardSelected = { boardId ->
                    navigation.onBoardSelected(BoardAddress(site = board.site, board = boardId))
                },
            ),
    )
