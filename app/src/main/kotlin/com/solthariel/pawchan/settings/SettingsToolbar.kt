package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CatalogSearchState
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.UiToolbarSearchField
import com.solthariel.pawchan.core.ui.UiToolbarSearchTransition

/** Catalog geometry, with the same heading typography as ordinary settings sections. */
@Composable
internal fun SettingsToolbar(
    title: String,
    search: CatalogSearchState,
    placeholder: String,
    back: (() -> Unit)?,
    inputDescription: String = placeholder,
    actions: @Composable RowScope.() -> Unit,
) {
    Box(Modifier.fillMaxWidth().height(APP_TOP_BAR_HEIGHT), contentAlignment = Alignment.Center) {
        Row(
            Modifier.fillMaxWidth().height(APP_TOOLBAR_BUTTON_SIZE).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (back != null || search.visible) {
                UiToolbarButton(UiIconKind.BACK, if (search.visible) "Закрыть поиск" else "Назад", onClick = {
                    if (search.visible) search.close() else back?.invoke()
                })
            } else {
                Spacer(Modifier.width(8.dp))
            }
            SettingsToolbarHeader(title, search, placeholder, inputDescription, Modifier.weight(1f))
            actions()
        }
    }
}

@Composable
private fun SettingsToolbarHeader(
    title: String,
    search: CatalogSearchState,
    placeholder: String,
    inputDescription: String,
    modifier: Modifier,
) {
    UiToolbarSearchTransition(search.visible, modifier, "settings-search") { active ->
        if (active) {
            UiToolbarSearchField(
                search.query,
                search::edit,
                search.focusRequester,
                Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                inputDescription,
            )
        } else {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    Modifier.weight(1f).padding(start = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                )
                UiToolbarButton(UiIconKind.SEARCH, placeholder, onClick = search::open)
            }
        }
    }
}
