package com.solthariel.pawchan.catalog

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
internal fun CatalogHiddenThreadItems(
    settings: CatalogHiddenThreadsMenu?,
    close: () -> Unit,
) {
    if (settings == null) return
    if (settings.count > 0) {
        DropdownMenuItem(
            text = {
                Text(if (settings.revealed) "Свернуть автоскрытые треды" else "Показать автоскрытые: ${settings.count}")
            },
            onClick = {
                settings.reveal?.invoke()
                close()
            },
        )
    }
}
