package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun CacheClearDialog(
    close: () -> Unit,
    clear: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = close,
        title = { Text("Очистить кэш изображений?") },
        text = {
            Text(
                "Миниатюры и кэш изображений будут удалены. " +
                    "Открытые медиа, сохранённые треды, черновики и скачанные документы останутся.",
            )
        },
        confirmButton = { TextButton(onClick = clear) { Text("Очистить") } },
        dismissButton = { TextButton(onClick = close) { Text("Отмена") } },
    )
}

@Composable
internal fun CacheNotice(
    text: String,
    close: () -> Unit,
) {
    TransientNotice(text, onDismiss = close)
}
