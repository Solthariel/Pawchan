package com.solthariel.pawchan.boards

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.catalog.BoardsActions
import com.solthariel.pawchan.catalog.CatalogError
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.BidirectionalPullRefreshBox
import com.solthariel.pawchan.core.ui.FloatingTopChromeState
import com.solthariel.pawchan.core.ui.LoadErrorPane
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.core.ui.PullRefreshOptions
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.core.ui.rememberFloatingTopChromeState
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.boards.resolveBoardAddressSearch
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.ui.APP_REFRESH_BOTTOM_INSET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun BoardsScreen(
    state: BoardsUiState,
    navigation: BoardsNavigation,
    actions: BoardsActions,
    address: BoardsAddressBindings,
    presentationCache: BoardsPresentationCache? = null,
    sourcePicker: @Composable (Modifier) -> Unit = {},
) {
    var query by rememberSaveable { mutableStateOf("") }
    var invalidQuery by rememberSaveable { mutableStateOf(false) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var category by rememberSaveable(address.site.value) { mutableStateOf("") }
    var invalidAttempt by remember { mutableLongStateOf(0L) }
    val listCache = presentationCache?.entries ?: remember { mutableMapOf<String, BoardsListUi>() }
    val fallbackChrome =
        rememberFloatingTopChromeState(APP_TOP_BAR_HEIGHT + SOURCE_PICKER_HEIGHT + CATEGORY_CHIPS_HEIGHT)
    val chrome = presentationCache?.chrome ?: fallbackChrome
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val boards = (state as? BoardsUiState.Content)?.boards.orEmpty()
    val categories = remember(boards) { boardCategories(boards) }
    val effectiveCategory =
        category.takeIf { selected -> selected.isEmpty() || categories.any { it.title == selected } }.orEmpty()
    val listCacheKey =
        if (query.isBlank()) "${address.site.value}\u0000$favoritesOnly\u0000$effectiveCategory" else null
    val submit = {
        invalidAttempt++
        invalidQuery = !submitBoardsQuery(query, boards, navigation, address, focus, keyboard)
    }
    val refreshing =
        state is BoardsUiState.Loading ||
            (state as? BoardsUiState.Content)?.isRefreshing == true ||
            (state as? BoardsUiState.Error)?.isRefreshing == true
    com.solthariel.pawchan.core.ui.FloatingTopChrome(state = chrome, toolbar = {
        BoardsSearchHeader(
            state = BoardsSearchBarState(query, invalidQuery, refreshing),
            invalidAttempt = invalidAttempt,
            favoritesOnly = favoritesOnly,
            actions =
                BoardsSearchBarActions(
                    back = navigation.onBack,
                    change = {
                        query = it
                        invalidQuery = false
                    },
                    submit = submit,
                    refresh = actions.onRetry,
                    toggleFavorites = { favoritesOnly = !favoritesOnly },
                ),
        )
        BoardsSourceFilterRow(sourcePicker = sourcePicker)
        if (state is BoardsUiState.Content) {
            BoardsCategoryChips(categories, effectiveCategory) { category = it }
        }
    }) {
        BoardsRefreshContent(
            state,
            BoardsListFilter(query, favoritesOnly, effectiveCategory),
            navigation,
            actions,
            Modifier.fillMaxSize(),
            BoardsListCache(listCache, listCacheKey),
        )
    }
}

@Composable
internal fun BoardsSourceFilterRow(sourcePicker: @Composable (Modifier) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BOARD_ROW_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        sourcePicker(Modifier.weight(1f))
    }
}

@Composable
private fun BoardsSearchHeader(
    state: BoardsSearchBarState,
    actions: BoardsSearchBarActions,
    invalidAttempt: Long,
    favoritesOnly: Boolean,
) {
    BoardsSearchField(state, actions, favoritesOnly)
    TransientNotice(
        stringResource(R.string.boards_address_invalid).takeIf { state.invalid },
        eventKey = invalidAttempt,
    )
}

internal data class BoardsSearchBarState(
    val query: String,
    val invalid: Boolean,
    val refreshing: Boolean,
)

internal data class BoardsSearchBarActions(
    val back: () -> Unit,
    val change: (String) -> Unit,
    val submit: () -> Unit,
    val refresh: () -> Unit,
    val toggleFavorites: () -> Unit,
)

@Composable
private fun BoardsRefreshContent(
    state: BoardsUiState,
    filter: BoardsListFilter,
    navigation: BoardsNavigation,
    actions: BoardsActions,
    modifier: Modifier,
    cache: BoardsListCache,
) {
    val refreshing =
        when (state) {
            is BoardsUiState.Content -> state.isRefreshing
            is BoardsUiState.Error -> state.isRefreshing
            BoardsUiState.Loading -> false
        }
    BidirectionalPullRefreshBox(
        refreshing = refreshing,
        onRefresh = actions.onRetry,
        options = PullRefreshOptions(allowBottom = true, bottomInset = APP_REFRESH_BOTTOM_INSET),
        modifier = modifier.forumSwipeGesture(navigation.onForumSwipe),
    ) {
        when (state) {
            BoardsUiState.Loading -> {
                val cached = cache.key?.let(cache.entries::get)
                if (cached == null) {
                    BoardsLoading()
                } else {
                    BoardsDirectoryList(
                        content = cached,
                        onExpandedChange = actions.onCategoryExpanded,
                        actions = BoardListActions(navigation.onBoardSelected, actions.onFavoriteChanged),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            is BoardsUiState.Error -> {
                LoadErrorPane(rateLimited = state.reason == CatalogError.RATE_LIMITED)
            }

            is BoardsUiState.Content -> {
                BoardsContent(
                    state = state,
                    filter = filter,
                    onBoardSelected = navigation.onBoardSelected,
                    actions = actions,
                    modifier = Modifier.fillMaxSize(),
                    cache = cache,
                )
            }
        }
    }
}

@Composable
private fun BoardsContent(
    state: BoardsUiState.Content,
    filter: BoardsListFilter,
    onBoardSelected: (BoardAddress) -> Unit,
    actions: BoardsActions,
    modifier: Modifier,
    cache: BoardsListCache,
) {
    val dimensions = boardListDimensions()
    val pixelDensity = LocalDensity.current.density
    val content by
        produceState<BoardsListUi?>(
            cache.key?.let(cache.entries::get),
            cache.key,
            state.boards,
            state.favoriteBoards,
            state.unavailableBoards,
            state.expandedCategories,
            filter.query,
            dimensions,
            pixelDensity,
            filter.favoritesOnly,
            filter.category,
        ) {
            if (filter.query.isNotBlank()) delay(BOARDS_SEARCH_DEBOUNCE_MILLIS)
            val prepared =
                withContext(Dispatchers.Default) {
                    state.prepareBoardsList(
                        filter.query,
                        dimensions,
                        pixelDensity,
                        filter.favoritesOnly,
                        filter.category,
                    )
                }
            cache.key?.let { cache.entries[it] = prepared }
            value = prepared
        }
    val prepared = content ?: cache.key?.let(cache.entries::get)
    if (prepared == null) {
        BoardsLoading()
    } else {
        BoardsDirectoryList(
            content = prepared,
            onExpandedChange = actions.onCategoryExpanded,
            actions = BoardListActions(onBoardSelected, actions.onFavoriteChanged),
            modifier = modifier,
        )
    }
}

@Composable
private fun BoardsLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        PageLoadingIndicator(description = "Загрузка разделов")
    }
}

private const val BOARDS_SEARCH_DEBOUNCE_MILLIS = 120L

internal data class BoardsNavigation(
    val onBoardSelected: (BoardAddress) -> Unit,
    val onBack: () -> Unit,
    val onForumSwipe: ((BoardSwipeDirection) -> Unit)? = null,
)

private data class BoardsListFilter(
    val query: String,
    val favoritesOnly: Boolean,
    val category: String,
)

private data class BoardsListCache(
    val entries: MutableMap<String, BoardsListUi>,
    val key: String?,
)

private fun submitBoardsQuery(
    query: String,
    boards: List<BoardDirectoryEntry>,
    navigation: BoardsNavigation,
    address: BoardsAddressBindings,
    focus: androidx.compose.ui.focus.FocusManager,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
): Boolean {
    val target = address.resolve(query)
    if (target != null) {
        openBoardsAddress(target, address, focus, keyboard)
        return true
    }
    val board = resolveBoardAddressSearch(query, boards, address.site)
    if (board != null) {
        focus.clearFocus()
        keyboard?.hide()
        navigation.onBoardSelected(board)
    }
    return board != null
}

private fun openBoardsAddress(
    target: BoardsAddressTarget,
    address: BoardsAddressBindings,
    focus: androidx.compose.ui.focus.FocusManager,
    keyboard: androidx.compose.ui.platform.SoftwareKeyboardController?,
) {
    focus.clearFocus()
    keyboard?.hide()
    address.open(target)
}

internal class BoardsPresentationCache(
    val entries: MutableMap<String, BoardsListUi>,
    val chrome: FloatingTopChromeState,
)
