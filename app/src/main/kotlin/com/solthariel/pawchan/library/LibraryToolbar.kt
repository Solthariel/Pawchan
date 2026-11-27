package com.solthariel.pawchan.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.catalog.CatalogRefreshButton
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.FocusWhenOpened
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.R.string as L

internal data class LibraryToolbarState(
    val title: String,
    val count: Int?,
    /** null — поиск закрыт. */
    val query: String?,
    val refreshing: Boolean,
)

internal data class LibraryToolbarActions(
    val onBack: () -> Unit,
    val onQuery: (String?) -> Unit,
    val onRefresh: (() -> Unit)? = null,
    val history: Boolean = false,
    val onHistoryCleared: () -> Unit = {},
    val extraMenu: (@Composable () -> Unit)? = null,
)

/** Верхняя панель списка: назад, заголовок со счётчиком, поиск и обновить; поиск — поле на месте заголовка. */
@Composable
internal fun LibraryToolbar(
    state: LibraryToolbarState,
    actions: LibraryToolbarActions,
) {
    val res = LocalResources.current

    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = APP_TOP_BAR_HEIGHT,
            ).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.query != null) {
            BackHandler { actions.onQuery(null) }
            CatalogIconButton(
                UiIconKind.BACK,
                res.getString(L.ui_close_search),
                onClick = { actions.onQuery(null) },
            )
            LibrarySearchInput(state.query, actions.onQuery)
        } else {
            CatalogIconButton(UiIconKind.BACK, res.getString(L.ui_back), onClick = actions.onBack)
            Text(
                state.title,
                Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            CatalogIconButton(UiIconKind.SEARCH, res.getString(L.ui_search), onClick = { actions.onQuery("") })
        }
        actions.onRefresh?.let { refresh ->
            CatalogRefreshButton(isRefreshing = state.refreshing, enabled = !state.refreshing, onClick = refresh)
        }
        if (actions.history) HistoryOverflowMenu(actions.onHistoryCleared)
        actions.extraMenu?.invoke()
    }
}

@Composable
internal fun RowScope.LibrarySearchInput(
    query: String,
    onQuery: (String?) -> Unit,
) {
    val focus = remember { FocusRequester() }
    FocusWhenOpened(focus)
    com.solthariel.pawchan.core.ui.UiToolbarSearchField(
        query,
        { onQuery(it) },
        focus,
        Modifier.weight(1f).padding(horizontal = 4.dp),
    )
}
