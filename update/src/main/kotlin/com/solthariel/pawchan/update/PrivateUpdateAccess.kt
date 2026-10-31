package com.solthariel.pawchan.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
internal fun PrivateUpdateAccess(
    store: UpdateTokenStore,
    onChanged: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val currentOnChanged = rememberUpdatedState(onChanged)
    val state = remember(store, scope) { PrivateUpdateAccessState(store, scope) { currentOnChanged.value(it) } }
    LaunchedEffect(state) { state.load() }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Закрытые обновления", style = MaterialTheme.typography.titleSmall)
        Text(if (state.configured) "Доступ сохранён на этом устройстве" else "Доступ к Solthariel/Pawchan не настроен")
        OutlinedTextField(
            value = state.token,
            onValueChange = { state.token = it },
            label = { Text("GitHub token — Contents: Read") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(autoCorrectEnabled = false, keyboardType = KeyboardType.Password),
            singleLine = true,
            enabled = !state.busy,
        )
        if (state.error) {
            Text(
                "Не удалось сохранить или прочитать доступ. Проверьте токен.",
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = state::save, enabled = !state.busy && state.token.isNotBlank()) {
                Text("Сохранить доступ")
            }
            TextButton(onClick = state::clear, enabled = !state.busy) { Text("Удалить доступ") }
        }
    }
}
