package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.backup.BackupSection
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun BackupSelectionDialog(
    restoring: Boolean,
    selected: Set<BackupSection>,
    change: (Set<BackupSection>) -> Unit,
    close: () -> Unit,
    confirm: () -> Unit,
    info: BackupSelectionInfo? = null,
) {
    val available = info?.available ?: BackupSection.entries.toSet()
    AlertDialog(
        onDismissRequest = close,
        title = { Text(if (restoring) "Что восстановить?" else "Что сохранить?") },
        text = {
            // AppAlertDialog already scrolls its text slot.
            Column {
                info?.details?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                if (restoring) {
                    Text(
                        "Выбранные разделы будут заменены. Пустой раздел очистит текущие данные. " +
                            "Остальное не изменится.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                BackupSection.entries.filter { it in available }.forEach { section ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .toggleable(section in selected, role = Role.Checkbox) { on ->
                                change(if (on) selected + section else selected - section)
                            }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            section.label + (info?.counts?.get(section)?.let { " ($it)" } ?: ""),
                            Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Checkbox(checked = section in selected, onCheckedChange = null, modifier = Modifier.size(24.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = selected.isNotEmpty() && available.containsAll(selected), onClick = confirm) {
                Text(if (restoring) "Восстановить" else "Сохранить")
            }
        },
        dismissButton = { TextButton(onClick = close) { Text("Отмена") } },
    )
}

internal data class BackupSelectionInfo(
    val available: Set<BackupSection>,
    val details: String,
    val counts: Map<BackupSection, Int>,
)
