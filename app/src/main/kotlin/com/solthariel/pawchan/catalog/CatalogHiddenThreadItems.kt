package com.solthariel.pawchan.catalog

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.R.string as L

@Composable
internal fun CatalogHiddenThreadItems(
    settings: CatalogHiddenThreadsMenu?,
    close: () -> Unit,
) {
    val res = LocalResources.current

    if (settings == null) return
    if (settings.count > 0) {
        DropdownMenuItem(
            text = {
                Text(
                    if (settings.revealed) {
                        res.getString(
                            L.ui_collapse_auto_hidden_threads,
                        )
                    } else {
                        res.getString(L.ui_show_auto_hidden, settings.count)
                    },
                )
            },
            onClick = {
                settings.reveal?.invoke()
                close()
            },
        )
    }
}
