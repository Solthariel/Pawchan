package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import com.solthariel.pawchan.core.ui.ModalDropdownMenu
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.R.string as L

@Composable
internal fun ProxyCatalogMenu() {
    val res = LocalResources.current

    var open by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    Box {
        UiToolbarButton(UiIconKind.MORE, res.getString(L.ui_proxy_menu), onClick = { open = true })
        ModalDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text(res.getString(L.ui_about_proxy)) }, onClick = {
                open = false
                help = true
            })
        }
    }
    if (help) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { help = false },
            title = { Text(res.getString(L.ui_proxy)) },
            text = {
                Text(res.getString(L.ui_only_one_enabled_proxy_can))
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = { help = false },
                ) { Text(res.getString(L.ui_got_it)) }
            },
        )
    }
}
