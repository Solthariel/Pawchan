package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.UiChoiceSegments
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.core.ui.UiToolbarButton
import com.solthariel.pawchan.domain.proxy.ProxyMode
import com.solthariel.pawchan.domain.proxy.ProxyService

@Composable
internal fun ProxyEditorFields(state: ProxyEditorState) {
    OutlinedTextField(
        state.draft.name,
        { state.draft = state.draft.copy(name = it) },
        Modifier.fillMaxWidth(),
        label = { Text("Название") },
        singleLine = true,
    )
    val modes = listOf(ProxyMode.HTTP, ProxyMode.HTTPS, ProxyMode.SOCKS5)
    UiChoiceSegments(listOf("HTTP", "HTTPS", "SOCKS5"), modes.indexOf(state.draft.mode), true) {
        state.draft = state.draft.copy(mode = modes[it])
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            state.host,
            state::changeHost,
            Modifier.weight(2f),
            label = { Text("Адрес") },
            singleLine = true,
        )
        OutlinedTextField(
            state.port,
            { state.port = it },
            Modifier.weight(1f),
            label = { Text("Порт") },
            singleLine = true,
        )
    }
    OutlinedTextField(
        state.login,
        { state.login = it },
        Modifier.fillMaxWidth(),
        label = { Text("Логин") },
        singleLine = true,
    )
    OutlinedTextField(
        state.password,
        { state.password = it },
        Modifier.fillMaxWidth(),
        label = { Text("Пароль") },
        singleLine = true,
        visualTransformation = if (state.passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            UiToolbarButton(
                if (state.passwordVisible) UiIconKind.HIDE else UiIconKind.SHOW,
                if (state.passwordVisible) "Скрыть пароль" else "Показать пароль",
            ) { state.passwordVisible = !state.passwordVisible }
        },
    )
    Text("Ресурсы", style = MaterialTheme.typography.titleMedium)
    ProxyResourceChoices(state)
}

@Composable
internal fun ProxyResourceChoices(state: ProxyEditorState) {
    SettingsGroupFrame(Modifier.testTag("proxy-resources"), horizontalInset = 0.dp) {
        ProxyService.entries.forEach { service ->
            val checked = service in state.draft.services
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .toggleable(checked, role = Role.Checkbox) { selected ->
                        val services = state.draft.services
                        val next = if (selected) services + service else services - service
                        state.draft = state.draft.copy(services = next)
                    }.semantics { contentDescription = "Ресурс ${service.proxyLabel}" }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(service.proxyLabel, Modifier.weight(1f))
                Checkbox(checked, onCheckedChange = null, modifier = Modifier.testTag("proxy-checkbox-${service.name}"))
            }
        }
    }
}
