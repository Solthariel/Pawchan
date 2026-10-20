package com.solthariel.pawchan.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.R
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarSearchTransition
import com.solthariel.pawchan.domain.model.BoardAddress

@Composable
internal fun CatalogToolbarRow(
    ui: CatalogToolbarRowUi,
    focusRequester: FocusRequester,
    actions: CatalogToolbarRowActions,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(CATALOG_TOOLBAR_CONTENT_HEIGHT)
                .padding(horizontal = CATALOG_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CatalogLeadingButton(ui.header.searchVisible, actions.onCloseSearch)
        CatalogAnimatedHeader(
            ui.header,
            focusRequester,
            actions.onQueryChange,
            actions.onOpenSearch,
            actions.overflow.boardLookup?.onOpen,
        )
        CatalogRefreshButton(ui.refreshing, actions.onRefresh)
        CatalogOverflowMenu(
            ui.menu.expanded,
            ui.menu.onExpandedChange,
            actions.overflow,
        )
    }
}

@Composable
private fun CatalogLeadingButton(
    searchVisible: Boolean,
    onCloseSearch: () -> Unit,
) {
    // Боковой панели больше нет: слева кнопка только у открытого поиска.
    if (searchVisible) {
        CatalogIconButton(
            kind = UiIconKind.BACK,
            description = stringResource(R.string.close_search),
            onClick = onCloseSearch,
        )
    } else {
        Spacer(Modifier.width(CATALOG_TITLE_START_INSET))
    }
}

private val CATALOG_TITLE_START_INSET = 8.dp

@Composable
private fun RowScope.CatalogAnimatedHeader(
    header: CatalogHeaderUi,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onChooseBoard: (() -> Unit)?,
) {
    UiToolbarSearchTransition(
        searchVisible = header.searchVisible,
        modifier = Modifier.weight(1f),
        label = "catalog-search",
    ) { active ->
        if (active) {
            CatalogSearchBar(
                query = header.query,
                onQueryChange = onQueryChange,
                focusRequester = focusRequester,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
            )
        } else {
            CatalogIdentityRow(header, onOpenSearch, onChooseBoard)
        }
    }
}

@Composable
private fun CatalogIdentityRow(
    header: CatalogHeaderUi,
    onOpenSearch: () -> Unit,
    onChooseBoard: (() -> Unit)?,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        CatalogBoardIdentity(
            board = header.board,
            title = header.title,
            boardSpeed = header.speed,
            modifier =
                Modifier
                    .weight(1f)
                    .then(
                        if (onChooseBoard != null) {
                            Modifier.clickable(
                                onClickLabel = stringResource(R.string.choose_board),
                                onClick = onChooseBoard,
                            )
                        } else {
                            Modifier
                        },
                    ).padding(start = 8.dp),
            switchable = onChooseBoard != null,
        )
        CatalogIconButton(
            kind = UiIconKind.SEARCH,
            description = stringResource(R.string.search_threads),
            onClick = onOpenSearch,
        )
    }
}

internal data class CatalogHeaderUi(
    val board: BoardAddress,
    val title: String?,
    val speed: Int?,
    val searchVisible: Boolean,
    val query: String,
)

internal data class CatalogMenuUi(
    val expanded: Boolean,
    val onExpandedChange: (Boolean) -> Unit,
)

internal data class CatalogToolbarRowUi(
    val header: CatalogHeaderUi,
    val refreshing: Boolean,
    val menu: CatalogMenuUi,
)

internal data class CatalogToolbarRowActions(
    val search: CatalogToolbarSearchActions,
    val navigation: CatalogToolbarNavigationActions,
    val overflow: CatalogMenuBindings,
) {
    val onQueryChange: (String) -> Unit
        get() = search.onQueryChange
    val onCloseSearch: () -> Unit
        get() = search.onCloseSearch
    val onOpenSearch: () -> Unit
        get() = search.onOpenSearch
    val onRefresh: () -> Unit
        get() = navigation.onRefresh
}

internal data class CatalogToolbarSearchActions(
    val onQueryChange: (String) -> Unit,
    val onCloseSearch: () -> Unit,
    val onOpenSearch: () -> Unit,
)

internal data class CatalogToolbarNavigationActions(
    val onRefresh: () -> Unit,
)

private val CATALOG_TOOLBAR_CONTENT_HEIGHT = 36.dp
private val CATALOG_HORIZONTAL_INSET = APP_TOOLBAR_HORIZONTAL_INSET
