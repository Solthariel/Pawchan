package com.solthariel.pawchan.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.catalog.CatalogRefreshButton
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarSearchField
import com.solthariel.pawchan.core.ui.UiTopAppBar

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
)

/** Верхняя панель списка: назад, заголовок со счётчиком, поиск и обновить; поиск — поле на месте заголовка. */
@Composable
internal fun LibraryToolbar(
    state: LibraryToolbarState,
    actions: LibraryToolbarActions,
) {
    if (state.query != null) BackHandler { actions.onQuery(null) }
    UiTopAppBar(
        title = {
            if (state.query != null) {
                Row(Modifier.fillMaxWidth()) { LibrarySearchInput(state.query, actions.onQuery) }
            } else {
                Text(
                    listOfNotNull(state.title, state.count?.takeIf { it > 0 }?.toString()).joinToString(" — "),
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        },
        navigationIcon = {
            CatalogIconButton(UiIconKind.BACK, if (state.query != null) "Закрыть поиск" else "Назад", onClick = {
                if (state.query != null) actions.onQuery(null) else actions.onBack()
            })
        },
        actions = {
            if (state.query == null) CatalogIconButton(UiIconKind.SEARCH, "Поиск", onClick = { actions.onQuery("") })
            if (actions.history) HistoryOverflowMenu(actions.onHistoryCleared)
            actions.onRefresh?.let { refresh ->
                CatalogRefreshButton(isRefreshing = state.refreshing, enabled = !state.refreshing, onClick = refresh)
            }
        },
    )
}

@Composable
internal fun RowScope.LibrarySearchInput(
    query: String,
    onQuery: (String?) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    UiToolbarSearchField(query, { onQuery(it) }, focus, Modifier.weight(1f))
}
