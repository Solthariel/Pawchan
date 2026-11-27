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

@Composable
internal fun ContentFilterOverflowMenu(actions: ContentFilterMenuActions) {
    val res = LocalResources.current

    var open by remember { mutableStateOf(false) }
    val close = { open = false }
    Box {
        UiToolbarButton(UiIconKind.MORE, res.getString(L.ui_auto_hide_menu), onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = close) {
            DropdownMenuItem(
                text = { Text(res.getString(L.ui_how_auto_hide_works)) },
                leadingIcon = { UiMenuIcon(UiIconKind.INFO) },
                onClick = {
                    close()
                    actions.help()
                },
            )
            DropdownMenuItem(
                text = { Text(res.getString(L.ui_statistics)) },
                leadingIcon = { UiMenuIcon(UiIconKind.CHART) },
                onClick = {
                    close()
                    actions.statistics()
                },
            )
            DropdownMenuItem(
                text = { Text(res.getString(L.ui_save_rules_to_txt)) },
                leadingIcon = { UiMenuIcon(UiIconKind.SAVE) },
                enabled = !actions.backup.busy,
                onClick = {
                    close()
                    actions.backup.save()
                },
            )
            DropdownMenuItem(
                text = { Text(res.getString(L.ui_restore_rules_from_txt)) },
                leadingIcon = { UiMenuIcon(UiIconKind.UPLOAD) },
                enabled = !actions.backup.busy && actions.backup.canRestore,
                onClick = {
                    close()
                    actions.backup.load()
                },
            )
        }
    }
}
