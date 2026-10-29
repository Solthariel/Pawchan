package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton

@Composable
internal fun UsagePeriodOptions(
    selected: Int,
    select: (Int) -> Unit,
) {
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        listOf("Сегодня", "Месяц", "Всё время").forEachIndexed { index, title ->
            FilterChip(
                selected == index,
                onClick = { select(index) },
                label = { Text(title) },
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}

@Composable
internal fun UsageStatisticsMenu(
    postCountEnabled: Boolean,
    onPostCount: (Boolean) -> Unit,
    reset: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        UiToolbarButton(UiIconKind.MORE, "Меню статистики", onClick = { open = true })
        DropdownMenu(open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text("/postcount — статистика при отправке") },
                trailingIcon = { androidx.compose.material3.Checkbox(postCountEnabled, onCheckedChange = null) },
                onClick = {
                    onPostCount(!postCountEnabled)
                    open = false
                },
            )
            DropdownMenuItem(text = { Text("Сбросить статистику") }, onClick = {
                open = false
                reset()
            })
        }
    }
}

@Composable
internal fun UsageResetDialog(
    close: () -> Unit,
    confirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Сбросить статистику?") },
        text = { Text("Все счётчики будут обнулены.") },
        confirmButton = { TextButton(onClick = confirm) { Text("Сбросить") } },
        dismissButton = { TextButton(onClick = close) { Text("Отмена") } },
    )
}
