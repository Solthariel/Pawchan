package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

@Composable
internal fun ContentFilterOverflowMenu(actions: ContentFilterMenuActions) {
    var open by remember { mutableStateOf(false) }
    val close = { open = false }
    Box {
        UiToolbarButton(UiIconKind.MORE, "Меню автоскрытия", onClick = { open = true })
        DropdownMenu(expanded = open, onDismissRequest = close) {
            DropdownMenuItem(text = { Text("Как работает автоскрытие") }, onClick = {
                close()
                actions.help()
            })
            DropdownMenuItem(text = { Text("Статистика") }, onClick = {
                close()
                actions.statistics()
            })
            DropdownMenuItem(text = { Text("Сохранить правила в TXT") }, enabled = !actions.backup.busy, onClick = {
                close()
                actions.backup.save()
            })
            DropdownMenuItem(
                text = { Text("Восстановить правила из TXT") },
                enabled = !actions.backup.busy && actions.backup.canRestore,
                onClick = {
                    close()
                    actions.backup.load()
                },
            )
        }
    }
}
