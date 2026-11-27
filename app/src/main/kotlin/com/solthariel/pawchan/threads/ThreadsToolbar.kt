package com.solthariel.pawchan.threads

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogIconButton
import com.solthariel.pawchan.catalog.CatalogRefreshButton
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.ActionMenuItem
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.library.LibrarySearchInput
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.ModalDropdownMenu as DropdownMenu

@Composable
internal fun ThreadsToolbar(
    refreshing: Boolean,
    query: String?,
    onQuery: (String?) -> Unit,
    actions: ThreadsScreenActions,
    hasPages: Boolean,
    toolbarActions: ThreadsToolbarActions,
) {
    val res = LocalResources.current

    var menuOpen by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(
                min = APP_TOP_BAR_HEIGHT,
            ).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (query != null) {
            BackHandler { onQuery(null) }
            CatalogIconButton(UiIconKind.BACK, res.getString(L.ui_close_search), onClick = { onQuery(null) })
            LibrarySearchInput(query, onQuery)
        } else {
            CatalogIconButton(UiIconKind.BACK, res.getString(L.ui_back), onClick = toolbarActions.onBack)
            Text(
                res.getString(L.ui_threads),
                Modifier.weight(1f).padding(start = 12.dp),
                style = MaterialTheme.typography.titleLarge,
            )
            CatalogIconButton(UiIconKind.SEARCH, res.getString(L.ui_search), onClick = { onQuery("") })
        }
        CatalogRefreshButton(isRefreshing = refreshing, enabled = !refreshing, onClick = actions.refresh)
        Box {
            CatalogIconButton(UiIconKind.MORE, res.getString(L.ui_menu), onClick = { menuOpen = true })
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                ActionMenuItem(res.getString(L.ui_list_display), {
                    menuOpen = false
                    toolbarActions.onDisplaySettings()
                }, icon = UiIconKind.PALETTE)
                toolbarActions.menuLinks.forEach { item ->
                    ActionMenuItem(res.threadsHubLabel(item), {
                        menuOpen = false
                        actions.links.open(item)
                    }, icon = item.linkIcon())
                }
                ActionMenuItem(res.getString(L.ui_close_all_pages), {
                    menuOpen = false
                    actions.pages.closeAll()
                }, enabled = hasPages, icon = UiIconKind.CLOSE)
            }
        }
    }
}

internal data class ThreadsToolbarActions(
    val onBack: () -> Unit,
    /** Ссылки, убранные с экрана в ⋮. */
    val menuLinks: List<com.solthariel.pawchan.domain.settings.ThreadsHubItem> = emptyList(),
    val onDisplaySettings: () -> Unit,
)
