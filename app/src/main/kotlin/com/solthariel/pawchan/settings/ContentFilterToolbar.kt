package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.catalog.CatalogSearchState
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ContentFilterToolbar(
    back: (() -> Unit)?,
    search: CatalogSearchState,
    actions: ContentFilterMenuActions,
) {
    val res = LocalResources.current

    SettingsToolbar(
        res.getString(L.ui_auto_hide),
        search,
        res.getString(L.ui_search_rules),
        back,
        inputDescription = res.getString(L.ui_find_rule),
    ) {
        UiToolbarButton(UiIconKind.ADD, res.getString(L.ui_add_rule), onClick = actions.add)
        ContentFilterOverflowMenu(actions)
    }
}
