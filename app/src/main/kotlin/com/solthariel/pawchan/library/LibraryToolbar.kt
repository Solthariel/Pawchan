package com.solthariel.pawchan.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.catalog.CatalogRefreshButton
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind

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
    Row(
        Modifier.fillMaxWidth().height(APP_TOP_BAR_HEIGHT).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (state.query != null) {
            BackHandler { actions.onQuery(null) }
            CatalogIconButton(UiIconKind.BACK, "Закрыть поиск", onClick = { actions.onQuery(null) })
            LibrarySearchInput(state.query, actions.onQuery)
        } else {
            CatalogIconButton(UiIconKind.BACK, "Назад", onClick = actions.onBack)
            Text(
                listOfNotNull(state.title, state.count?.takeIf { it > 0 }?.toString()).joinToString(" · "),
                Modifier.weight(1f).padding(start = 4.dp),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
            )
            CatalogIconButton(UiIconKind.SEARCH, "Поиск", onClick = { actions.onQuery("") })
        }
        if (actions.history) HistoryOverflowMenu(actions.onHistoryCleared)
        actions.onRefresh?.let { refresh ->
            CatalogRefreshButton(isRefreshing = state.refreshing, enabled = !state.refreshing, onClick = refresh)
        }
    }
}

@Composable
internal fun RowScope.LibrarySearchInput(
    query: String,
    onQuery: (String?) -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    Box(
        Modifier
            .weight(1f)
            .height(34.dp)
            .padding(horizontal = 4.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(17.dp))
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        if (query.isEmpty()) {
            Text("Поиск", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        }
        BasicTextField(
            query,
            onValueChange = { onQuery(it) },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        )
    }
}
