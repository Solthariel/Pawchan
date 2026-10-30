package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.core.ui.UiTopAppBar

@Composable
internal fun SettingsPageToolbar(
    title: String,
    back: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    UiTopAppBar(
        title = { Text(title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis) },
        navigationIcon = { UiToolbarButton(UiIconKind.BACK, "Назад", onClick = back) },
        actions = actions,
    )
}
