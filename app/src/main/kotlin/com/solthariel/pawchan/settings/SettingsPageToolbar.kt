package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_HORIZONTAL_INSET
import com.solthariel.pawchan.core.ui.APP_TOP_BAR_HEIGHT
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

@Composable
internal fun SettingsPageToolbar(
    title: String,
    back: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().height(APP_TOP_BAR_HEIGHT).padding(horizontal = APP_TOOLBAR_HORIZONTAL_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        UiToolbarButton(UiIconKind.BACK, "Назад", onClick = back)
        Text(title, Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.titleLarge)
        actions()
    }
}
