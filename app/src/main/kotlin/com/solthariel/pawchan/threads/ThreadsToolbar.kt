package com.solthariel.pawchan.threads

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.catalog.CatalogRefreshButton
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiTopAppBar
import com.solthariel.pawchan.library.LibrarySearchInput

@Composable
internal fun ThreadsToolbar(
    refreshing: Boolean,
    query: String?,
    onQuery: (String?) -> Unit,
    actions: ThreadsScreenActions,
    hasPages: Boolean,
    onDisplaySettings: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    if (query != null) BackHandler { onQuery(null) }
    UiTopAppBar(
        title = {
            if (query != null) {
                Row(Modifier.fillMaxWidth()) { LibrarySearchInput(query, onQuery) }
            } else {
                Text("Треды", style = MaterialTheme.typography.titleLarge)
            }
        },
        navigationIcon = {
            if (query != null) CatalogIconButton(UiIconKind.BACK, "Закрыть поиск", onClick = { onQuery(null) })
        },
        actions = {
            if (query == null) CatalogIconButton(UiIconKind.SEARCH, "Поиск", onClick = { onQuery("") })
            CatalogRefreshButton(isRefreshing = refreshing, enabled = !refreshing, onClick = actions.refresh)
            Box {
                CatalogIconButton(UiIconKind.MORE, "Меню", onClick = { menuOpen = true })
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    ActionMenuItem("Отображение списков", {
                        menuOpen = false
                        onDisplaySettings()
                    })
                    ActionMenuItem("Закрыть все страницы", {
                        menuOpen = false
                        actions.pages.closeAll()
                    }, enabled = hasPages)
                }
            }
        },
    )
}
