package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue

/** Explicit confirmation keeps invalid or cancelled input out of saved settings. */
@Composable
fun NumberInputDialog(
    title: String,
    value: Int,
    range: IntRange,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var input by rememberSaveable(value, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(value.toString(), TextRange(0, value.toString().length)))
    }
    val focus = remember { FocusRequester() }
    val number = input.text.toIntOrNull()?.takeIf { it in range }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        keyboardStable = true,
        title = { Text(title) },
        text = {
            Column {
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(Unit) {
                    focus.requestFocus()
                    keyboard?.show()
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = {
                        if (it.text.length <= range.last.toString().length && it.text.all(Char::isDigit)) {
                            input = it
                        }
                    },
                    modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("number-input"),
                    label = { Text("Значение") },
                    singleLine = true,
                    isError = number == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { number?.let(onConfirm) }),
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
