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
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiMenuIcon
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.R.string as L
import com.solthariel.pawchan.core.ui.ModalDropdownMenu as DropdownMenu

/** Direct navigation only: no placeholder actions or destructive settings resets. */
@Composable
internal fun SettingsOverflowMenu(onOpen: (SettingsSection?) -> Unit) {
    val res = LocalResources.current

    var expanded by remember { mutableStateOf(false) }
    Box {
        UiToolbarButton(UiIconKind.MORE, res.getString(L.ui_settings_menu), onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = {
                Text(res.getString(L.ui_all_sections))
            }, leadingIcon = { UiMenuIcon(UiIconKind.GRID) }, onClick = {
                expanded = false
                onOpen(null)
            })
            SettingsSection.entries.forEach { section ->
                DropdownMenuItem(text = {
                    Text(
                        androidx.compose.ui.platform.LocalResources.current
                            .settingsLabel(section.title),
                    )
                }, leadingIcon = { UiMenuIcon(section.icon) }, onClick = {
                    expanded = false
                    onOpen(section)
                })
            }
        }
    }
}
