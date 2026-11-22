package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.domain.drafts.MAX_IMAGE_TEXT_LENGTH

@Composable
internal fun ImageTextDialog(
    onDismiss: () -> Unit,
    initialText: String = "",
    onConfirm: (String) -> Unit,
) {
    var text by remember { mutableStateOf(initialText) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialText.isEmpty()) "Добавить текст" else "Изменить текст") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(MAX_IMAGE_TEXT_LENGTH) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Текст") },
            )
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        confirmButton = { TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) { Text("Готово") } },
    )
}
