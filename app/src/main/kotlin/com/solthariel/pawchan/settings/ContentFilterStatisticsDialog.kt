package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.filter.ContentFilterStatistics
import com.solthariel.pawchan.domain.filter.ContentFilterStatisticsRepository
import com.solthariel.pawchan.domain.model.DomainOutcome
import kotlinx.coroutines.launch
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun ContentFilterStatisticsDialog(
    statistics: ContentFilterStatistics,
    repository: ContentFilterStatisticsRepository?,
    close: () -> Unit,
) {
    var confirm by remember { mutableStateOf(false) }
    var resetting by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Статистика автоскрытия") },
        text = { ContentFilterStatisticsBody(statistics, failed) },
        confirmButton = { TextButton(onClick = close) { Text("Закрыть") } },
        dismissButton = {
            TextButton(
                enabled = repository != null && statistics.available && !resetting,
                onClick = { confirm = true },
            ) {
                Text("Сбросить")
            }
        },
    )
    if (confirm) {
        FilterStatisticsResetConfirmation({ confirm = false }) {
            confirm = false
            scope.launch {
                resetting = true
                try {
                    failed = repository?.reset() !is DomainOutcome.Success
                } finally {
                    resetting = false
                }
            }
        }
    }
}

@Composable
private fun FilterStatisticsResetConfirmation(
    close: () -> Unit,
    reset: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = close,
        title = { Text("Сбросить статистику?") },
        text = { Text("Счётчики обнулятся. Правила и настройка скрытых тредов останутся без изменений.") },
        confirmButton = { TextButton(onClick = reset) { Text("Сбросить счётчики") } },
        dismissButton = { TextButton(onClick = close) { Text("Отмена") } },
    )
}
