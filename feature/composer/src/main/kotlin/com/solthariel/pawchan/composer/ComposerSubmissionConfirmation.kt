package com.solthariel.pawchan.composer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
internal fun confirmedCaptchaReload(
    failure: ComposerFailureState,
    onReload: () -> Unit,
): () -> Unit {
    var confirming by remember(failure.error) { mutableStateOf(false) }
    if (confirming && failure.error == ReplyError.SUBMISSION_UNCONFIRMED) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Проверили результат отправки?") },
            text = {
                Text(
                    "Сервер мог принять сообщение. Проверьте тред или каталог. " +
                        "Повторная отправка может создать дубликат. " +
                        "Продолжение только загрузит капчу — сообщение не отправится.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirming = false
                    onReload()
                }) { Text("Подготовить повторную попытку") }
            },
            dismissButton = {
                TextButton(onClick = { confirming = false }) { Text("Отмена") }
            },
        )
    }
    return {
        if (failure.error == ReplyError.SUBMISSION_UNCONFIRMED) confirming = true else onReload()
    }
}
