package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import com.solthariel.pawchan.catalog.CatalogSearchState
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

@Composable
internal fun ContentFilterToolbar(
    back: (() -> Unit)?,
    search: CatalogSearchState,
    actions: ContentFilterMenuActions,
) {
    SettingsToolbar("Автоскрытие", search, "Поиск правил", back, inputDescription = "Найти правило") {
        UiToolbarButton(UiIconKind.ADD, "Добавить правило", onClick = actions.add)
        ContentFilterOverflowMenu(actions)
    }
}
