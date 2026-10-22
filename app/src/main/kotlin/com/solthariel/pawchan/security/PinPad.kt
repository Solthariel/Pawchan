package com.solthariel.pawchan.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.AppDialogStyle

@Composable
internal fun PinPad(
    title: String,
    state: AppLockState,
    submit: (String) -> Unit,
) {
    val input = rememberPinUnlockInput(state)
    val digits = input.digits
    val enabled = state.ready && !state.busy && state.retrySeconds == 0 && !state.biometricSetupReady
    Column(
        Modifier.widthIn(max = 340.dp).fillMaxWidth().padding(AppDialogStyle.contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(
            "●".repeat(digits.length) + "○".repeat(PIN_LENGTH - digits.length),
            Modifier.semantics { contentDescription = "Введено цифр: ${digits.length} из $PIN_LENGTH" },
            style = MaterialTheme.typography.headlineMedium,
        )
        PinStatus(state)
        listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"), listOf("", "0", "⌫"))
            .forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    row.forEach { digit ->
                        TextButton(
                            enabled = digit.isNotEmpty() && enabled,
                            modifier =
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 64.dp)
                                    .semantics { if (digit == "⌫") contentDescription = "Удалить цифру" },
                            onClick = {
                                val value = if (digit == "⌫") digits.dropLast(1) else (digits + digit).take(PIN_LENGTH)
                                input.change(value, enabled, submit)
                            },
                        ) { Text(digit, style = MaterialTheme.typography.headlineSmall) }
                    }
                }
            }
    }
}

@Composable
private fun PinStatus(state: AppLockState) {
    Text(
        when {
            !state.ready -> "Загрузка…"
            state.busy -> "Проверка…"
            state.retrySeconds > 0 -> "Повторите через ${state.retrySeconds} с"
            else -> state.error.orEmpty()
        },
        modifier = Modifier.fillMaxWidth(),
        color = if (state.busy) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodyMedium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        minLines = 2,
        maxLines = 2,
    )
}
