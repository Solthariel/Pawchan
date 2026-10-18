package com.solthariel.pawchan.boards

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.catalog.BoardsActions
import com.solthariel.pawchan.catalog.CatalogError
import com.solthariel.pawchan.core.ui.BidirectionalPullRefreshBox
import com.solthariel.pawchan.core.ui.LoadErrorPane
import com.solthariel.pawchan.core.ui.PullRefreshOptions
import com.solthariel.pawchan.domain.boards.resolveBoardSearch
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.ui.CATALOG_REFRESH_BOTTOM_INSET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
internal fun BoardsScreen(
    state: BoardsUiState,
    onBoardSelected: (BoardId) -> Unit,
    onBack: () -> Unit,
    actions: BoardsActions,
    modifier: Modifier = Modifier,
    address: BoardsAddressBindings? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var invalidQuery by rememberSaveable { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val boards = (state as? BoardsUiState.Content)?.boards.orEmpty()
    val target = remember(query, address) { address?.resolve(query) }
    val openTarget = { opened: BoardsAddressTarget ->
        focus.clearFocus()
        keyboard?.hide()
        address?.open?.invoke(opened)
        Unit
    }
    val submit = {
        if (target != null) {
            openTarget(target)
        } else {
            val board = resolveBoardSearch(query, boards)
            invalidQuery = board == null
            if (board != null) {
                focus.clearFocus()
                keyboard?.hide()
                onBoardSelected(board)
            }
        }
    }
    val refreshing =
        state is BoardsUiState.Loading ||
            (state as? BoardsUiState.Content)?.isRefreshing == true ||
            (state as? BoardsUiState.Error)?.isRefreshing == true
    Column(modifier = modifier.fillMaxSize()) {
        BoardsTopBar(
            state = BoardsSearchBarState(query, invalidQuery, refreshing),
            actions =
                BoardsSearchBarActions(
                    back = onBack,
                    change = {
                        query = it
                        invalidQuery = false
                    },
                    submit = submit,
                    refresh = actions.onRetry,
                ),
        )
        if (invalidQuery) {
            Text(
                stringResource(R.string.boards_address_invalid),
                Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        BoardsRefreshContent(state, query, onBoardSelected, actions)
    }
}

@Composable
private fun BoardsTopBar(
    state: BoardsSearchBarState,
    actions: BoardsSearchBarActions,
) {
    BoardsSearchField(state, actions)
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
)

@Composable
private fun ColumnScope.BoardsRefreshContent(
    state: BoardsUiState,
    query: String,
    onBoardSelected: (BoardId) -> Unit,
    actions: BoardsActions,
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
        options = PullRefreshOptions(allowBottom = true, bottomInset = CATALOG_REFRESH_BOTTOM_INSET),
        modifier = Modifier.weight(1f),
    ) {
        when (state) {
            BoardsUiState.Loading -> {
                Unit
            }

            is BoardsUiState.Error -> {
                LoadErrorPane(rateLimited = state.reason == CatalogError.RATE_LIMITED)
            }

            is BoardsUiState.Content -> {
                BoardsContent(
                    state = state,
                    query = query,
                    onBoardSelected = onBoardSelected,
                    actions = actions,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Composable
private fun BoardsContent(
    state: BoardsUiState.Content,
    query: String,
    onBoardSelected: (BoardId) -> Unit,
    actions: BoardsActions,
    modifier: Modifier,
) {
    val dimensions = boardListDimensions()
    val pixelDensity = LocalDensity.current.density
    val favoriteLabel = stringResource(R.string.favorite_boards)
    val content by produceState<BoardsListUi?>(null, state, query, dimensions, pixelDensity, favoriteLabel) {
        if (query.isNotBlank()) delay(BOARDS_SEARCH_DEBOUNCE_MILLIS)
        value =
            withContext(Dispatchers.Default) {
                state.prepareBoardsList(query, favoriteLabel, dimensions, pixelDensity)
            }
    }
    content?.let { prepared ->
        BoardsDirectoryList(
            content = prepared,
            onExpandedChange = actions.onCategoryExpanded,
            actions = BoardListActions(onBoardSelected, actions.onFavoriteChanged),
            modifier = modifier,
        )
    }
}

private const val BOARDS_SEARCH_DEBOUNCE_MILLIS = 120L
