package com.solthariel.pawchan.catalog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import com.solthariel.pawchan.core.ui.BidirectionalPullRefreshBox
import com.solthariel.pawchan.core.ui.LoadErrorPane
import com.solthariel.pawchan.core.ui.PullRefreshOptions
import com.solthariel.pawchan.core.ui.RefreshFailureNotice
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.thumbnail.ThumbnailLoader
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import com.solthariel.pawchan.ui.APP_REFRESH_BOTTOM_INSET

@Composable
internal fun CatalogScreen(
    presentation: CatalogPresentation,
    actions: CatalogActions,
    bottomActions: AppBottomActions,
    menu: CatalogMenuBindings,
) {
    val filtered = presentation.state as? CatalogUiState.Content
    val imageLoader = presentation.imageLoader
    val thumbnailLoader = presentation.thumbnailLoader
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val attachmentsSession = remember(presentation.board) { CatalogAttachmentSession() }
    val threadMenu = rememberCatalogThreadMenu(actions.onThreadClick, actions.threadMenuHost, actions.onAddRule)
    val screenActions =
        CatalogScreenActions(
            catalog =
                actions.copy(
                    content =
                        actions.content.copy(onThreadClick = threadMenu.onClick, onRefresh = {
                            thumbnailLoader.retryVisible()
                            actions.onRefresh()
                        }),
                ),
            chooseAttachments = { attachmentsSession.openOrChoose(it, actions.onMediaClick) },
            threadMenu = threadMenu.onLongClick,
        )
    val hiddenMenu =
        menu.hiddenThreads?.copy(
            reveal = actions.onToggleFiltered,
            revealed = filtered?.filtersRevealed == true,
            count = filtered?.autoHiddenThreads?.size ?: 0,
        )
    CatalogWorkspace(
        presentation = presentation,
        searchQuery = searchQuery,
        onSearchQueryChange = { searchQuery = it },
        actions = screenActions,
        bottomActions = bottomActions,
        menu = menu.copy(hiddenThreads = hiddenMenu),
    )
    CatalogMenuDialogs(menu)
    if (attachmentsSession.visible) {
        CatalogAttachmentPicker(
            attachments = requireNotNull(attachmentsSession.choices),
            imageLoader = imageLoader,
            onDismiss = attachmentsSession::dismiss,
            onSelected = { attachmentsSession.select(it, actions.onMediaClick) },
            onLongPress = actions.content.onMediaLongClick,
        )
    }
}

@Composable
private fun CatalogWorkspace(
    presentation: CatalogPresentation,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    actions: CatalogScreenActions,
    bottomActions: AppBottomActions,
    menu: CatalogMenuBindings,
) {
    AppNavigationWorkspace(
        actions = bottomActions,
        selected = AppBottomDestination.CATALOG,
    ) {
        CatalogThreadsPane(presentation, searchQuery, onSearchQueryChange, actions, menu)
    }
}

@Composable
private fun CatalogThreadsPane(
    presentation: CatalogPresentation,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    actions: CatalogScreenActions,
    menu: CatalogMenuBindings,
) {
    val updateSearchQuery = { query: String ->
        onSearchQueryChange(query)
        actions.catalog.onSearchQueryChange(query)
    }
    CatalogBoardLookupLayer(menu.boardLookup) {
        Column(modifier = Modifier.fillMaxSize()) {
            CatalogToolbar(
                input =
                    CatalogToolbarInput(
                        board = presentation.board,
                        state = presentation.state,
                        searchQuery = searchQuery,
                    ),
                onSearchQueryChange = updateSearchQuery,
                actions = actions.catalog,
                menu = menu,
            )
            RefreshFailureNotice(
                (presentation.state as? CatalogUiState.Content)?.refreshFailure,
                actions.catalog.onChallenge,
            )
            CatalogBody(
                presentation = presentation,
                actions = actions,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun CatalogBody(
    presentation: CatalogPresentation,
    actions: CatalogScreenActions,
    modifier: Modifier,
) {
    val refreshing =
        presentation.state is CatalogUiState.Loading ||
            (presentation.state as? CatalogUiState.Content)?.isRefreshing == true
    BidirectionalPullRefreshBox(
        refreshing = refreshing,
        onRefresh = actions.catalog.onRefresh,
        options = PullRefreshOptions(allowBottom = true, bottomInset = APP_REFRESH_BOTTOM_INSET),
        modifier = modifier,
    ) {
        when (val state = presentation.state) {
            CatalogUiState.Loading -> {
                // Initial loading intentionally keeps the content area empty.
            }

            is CatalogUiState.Content -> {
                CatalogContent(
                    state = state,
                    thumbnailLoader = presentation.thumbnailLoader,
                    onThreadClick = actions.catalog.onThreadClick,
                    onMediaClick = actions.chooseAttachments,
                    onThreadLongClick = actions.threadMenu,
                    onMediaLongClick = actions.catalog.content.onMediaLongClick,
                )
            }

            is CatalogUiState.Error -> {
                LoadErrorPane(
                    challengeUrl = state.challengeUrl,
                    rateLimited = state.reason == CatalogError.RATE_LIMITED,
                    onChallenge = actions.catalog.onChallenge,
                )
            }
        }
    }
}

internal data class CatalogAutoHideControls(
    val count: Int,
    val revealed: Boolean,
    val toggle: () -> Unit,
)

internal data class CatalogPresentation(
    val board: BoardAddress,
    val state: CatalogUiState,
    val imageLoader: ImageLoader,
    val thumbnailLoader: ThumbnailLoader,
    val autoHideControls: CatalogAutoHideControls? = null,
)

private data class CatalogScreenActions(
    val catalog: CatalogActions,
    val chooseAttachments: (List<MediaAttachmentUi>) -> Unit,
    val threadMenu: (CatalogThreadUi) -> Unit,
)
