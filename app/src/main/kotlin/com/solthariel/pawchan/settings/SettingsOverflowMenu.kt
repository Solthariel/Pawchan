package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

/** Direct navigation only: no placeholder actions or destructive settings resets. */
@Composable
internal fun SettingsOverflowMenu(onOpen: (SettingsSection?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        UiToolbarButton(UiIconKind.MORE, "Меню настроек", onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Все разделы") }, onClick = {
                expanded = false
                onOpen(null)
            })
            SettingsSection.entries.forEach { section ->
                DropdownMenuItem(text = { Text(section.title) }, onClick = {
                    expanded = false
                    onOpen(section)
                })
            }
        }
    }
}
