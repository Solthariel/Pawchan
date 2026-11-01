package com.solthariel.pawchan.library

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.library.HistoryCleanup
import com.solthariel.pawchan.domain.model.DomainOutcome
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.ReadingPreferences
import com.solthariel.pawchan.workspace.LocalWorkspace
import kotlinx.coroutines.launch
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun ClearVisitHistory(onCleared: () -> Unit) {
    val cleanup = LocalWorkspace.current?.library as? HistoryCleanup ?: return
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    androidx.compose.material3.DropdownMenuItem(
        text = { Text(message ?: "Очистить историю") },
        enabled = !busy,
        onClick = { confirm = true },
    )
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text("Очистить историю посещений?") },
            text = { Text("Избранное, открытые страницы, черновики и сохранённые треды останутся.") },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    busy = true
                    scope.launch {
                        message =
                            if (cleanup.clearVisitHistory() is DomainOutcome.Success) {
                                onCleared()
                                "История очищена"
                            } else {
                                "Не удалось очистить историю"
                            }
                        busy = false
                    }
                }) { Text("Очистить") }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Отмена") } },
        )
    }
}
