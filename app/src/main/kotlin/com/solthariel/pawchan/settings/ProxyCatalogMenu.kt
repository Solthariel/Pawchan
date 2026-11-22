package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.core.ui.ModalDropdownMenu
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

@Composable
internal fun ProxyCatalogMenu() {
    var open by remember { mutableStateOf(false) }
    var help by remember { mutableStateOf(false) }
    Box {
        UiToolbarButton(UiIconKind.MORE, "Меню прокси", onClick = { open = true })
        ModalDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(text = { Text("О прокси") }, onClick = {
                open = false
                help = true
            })
        }
    }
    if (help) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { help = false },
            title = { Text("Прокси") },
            text = {
                Text("Одному ресурсу можно назначить один включённый прокси. HTTP не шифрует соединение до прокси.")
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { help = false }) { Text("Понятно") } },
        )
    }
}
