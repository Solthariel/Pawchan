package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.solthariel.pawchan.core.ui.TransientNotice
import com.solthariel.pawchan.data.maintenance.BackupProtection
import com.solthariel.pawchan.core.ui.AppAlertDialog as AlertDialog

@Composable
internal fun BackupPasswordDialog(
    creating: Boolean,
    busy: Boolean,
    error: String?,
    close: () -> Unit,
    confirm: (CharArray?) -> Unit,
) {
    val input = remember { BackupPasswordInput() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, input) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) input.clear() }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            input.clear()
        }
    }
    AlertDialog(
        onDismissRequest = { if (!busy) close() },
        properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
        title = { Text(if (creating) "Защита резервной копии" else "Пароль резервной копии") },
        text = { BackupPasswordFields(input, creating, busy, error) },
        confirmButton = {
            TextButton(enabled = !busy && input.valid(creating), onClick = {
                val password = if (!creating || input.encrypted) input.password.toCharArray() else null
                input.clear()
                confirm(password)
            }) {
                Text(
                    if (busy) {
                        "Обработка…"
                    } else if (creating) {
                        "Продолжить"
                    } else {
                        "Открыть"
                    },
                )
            }
        },
        dismissButton = { TextButton(enabled = !busy, onClick = close) { Text("Отмена") } },
    )
}

@Composable
private fun BackupPasswordFields(
    input: BackupPasswordInput,
    creating: Boolean,
    busy: Boolean,
    error: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (creating) {
            Row {
                Checkbox(checked = input.encrypted, enabled = !busy, onCheckedChange = {
                    input.encrypted = it
                    input.clear()
                })
                Text("Защитить копию паролем", Modifier.padding(top = 12.dp))
            }
        }
        if (!creating || input.encrypted) {
            OutlinedTextField(
                value = input.password,
                onValueChange = { input.password = it },
                modifier = Modifier.fillMaxWidth(),
                enabled = !busy,
                label = { Text("Пароль") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                singleLine = true,
            )
            if (creating) {
                OutlinedTextField(
                    value = input.repeated,
                    onValueChange = { input.repeated = it },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                    label = { Text("Повторите пароль") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false),
                    singleLine = true,
                )
            }
        }
        TransientNotice(error)
    }
}

private class BackupPasswordInput {
    var password by mutableStateOf("")
    var repeated by mutableStateOf("")
    var encrypted by mutableStateOf(true)

    fun valid(creating: Boolean): Boolean =
        if (!creating) {
            password.isNotEmpty()
        } else {
            (
                !encrypted ||
                    (password.length >= BackupProtection.MIN_PASSWORD_CHARS && password == repeated)
            )
        }

    fun clear() {
        password = ""
        repeated = ""
    }
}
