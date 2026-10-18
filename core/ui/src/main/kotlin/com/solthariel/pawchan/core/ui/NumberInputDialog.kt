package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType

/** Explicit confirmation keeps invalid or cancelled input out of saved settings. */
@Composable
fun NumberInputDialog(
    title: String,
    value: Int,
    range: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var input by rememberSaveable(value) { mutableStateOf(value.toString()) }
    val number = input.toIntOrNull()?.takeIf { it in range }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        if (it.length <= range.last.toString().length && it.all(Char::isDigit)) {
                            input = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Значение") },
                    singleLine = true,
                    isError = number == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    supportingText = { Text("От ${range.first} до ${range.last}") },
                )
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } },
        confirmButton = {
            TextButton(modifier = Modifier.testTag("number-confirm"), enabled = number != null, onClick = {
                number?.let(onConfirm)
            }) { Text("Применить") }
        },
    )
}
