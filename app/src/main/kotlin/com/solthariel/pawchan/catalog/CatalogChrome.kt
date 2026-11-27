package com.solthariel.pawchan.catalog

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.navigation.LocalSelectedForum
import com.solthariel.pawchan.R.string as L

@Composable
internal fun CatalogToolbar(
    input: CatalogToolbarInput,
    onSearchQueryChange: (String) -> Unit,
    actions: CatalogActions,
    menu: CatalogMenuBindings,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = APP_TOP_BAR_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        CatalogToolbarHeader(
            input = input,
            onSearchQueryChange = onSearchQueryChange,
            actions = actions,
            menu = menu,
        )
    }
}

@Composable
private fun CatalogToolbarHeader(
    input: CatalogToolbarInput,
    onSearchQueryChange: (String) -> Unit,
    actions: CatalogActions,
    menu: CatalogMenuBindings,
) {
    val res = LocalResources.current

    var menuVisible by rememberSaveable { mutableStateOf(false) }
    val search = rememberCatalogSearchState(input.searchQuery, onSearchQueryChange)
    val lookup = menu.boardLookup
    if (lookup?.visible == true) {
        val selectedSite = LocalSelectedForum.current?.value ?: input.board.site.value
        val supportsTags = selectedSite == input.board.site.value && menu.capabilities.tags
        CatalogBoardLookupToolbar(
            lookup,
            if (supportsTags) res.getString(L.ui_enter_board_or_tag) else res.getString(L.ui_enter_board),
        )
        return
    }
    val searchVisible = search.visible
    val content = input.state as? CatalogUiState.Content
    CatalogToolbarRow(
        ui =
            CatalogToolbarRowUi(
                header =
                    CatalogHeaderUi(
                        board = input.board,
                        title = content?.boardTitle,
                        speed = content?.boardSpeed,
                        searchVisible = searchVisible,
                        query = input.searchQuery,
                        speedSupported = menu.capabilities.boardSpeed,
                    ),
                refreshing = input.state is CatalogUiState.Loading || content?.isRefreshing == true,
                menu = CatalogMenuUi(menuVisible, { menuVisible = it }),
            ),
        focusRequester = search.focusRequester,
        actions =
            CatalogToolbarRowActions(
                search =
                    CatalogToolbarSearchActions(
                        onQueryChange = search::edit,
                        onCloseSearch = search::close,
                        onOpenSearch = search::open,
                    ),
                overflow = menu,
                navigation =
                    CatalogToolbarNavigationActions(
                        onRefresh = actions.onRefresh,
                    ),
            ),
    )
}

internal data class CatalogToolbarInput(
    val board: BoardAddress,
    val state: CatalogUiState,
    val searchQuery: String,
)
