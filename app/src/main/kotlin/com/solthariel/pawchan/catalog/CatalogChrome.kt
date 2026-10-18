package com.solthariel.pawchan.catalog

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.domain.model.BoardAddress

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
                .height(APP_TOP_BAR_HEIGHT),
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
    var menuVisible by rememberSaveable { mutableStateOf(false) }
    val search = rememberCatalogSearchState(input.searchQuery, onSearchQueryChange)
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
