package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.VocarooDns
import com.solthariel.pawchan.domain.settings.VocarooDnsProvider
import com.solthariel.pawchan.infrastructure.vocaroo.customDohUrl

/** «DNS для Vocaroo»: пресеты DoH и свой адрес; действует только на записи Vocaroo. */
internal fun vocarooDnsItems(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsItem> {
    val dns = app.behavior.vocarooDns
    val providers = VocarooDnsProvider.entries
    val choice =
        SettingsItem.Choice(
            "DNS для Vocaroo",
            providers.map(VocarooDnsProvider::label),
            providers.indexOf(dns.provider),
        ) { index ->
            update { it.withVocarooDns { dns -> dns.copy(provider = providers[index]) } }
        }
    if (dns.provider != VocarooDnsProvider.CUSTOM) return listOf(choice)
    val custom =
        SettingsItem.Custom("Адрес DoH для Vocaroo") {
            VocarooDohField(dns.customUrl) { url ->
                update { it.withVocarooDns { dns -> dns.copy(customUrl = url) } }
            }
        }
    return listOf(choice, custom)
}

@Composable
private fun VocarooDohField(
    saved: String,
    onSave: (String) -> Unit,
) {
    var text by rememberSaveable(saved) { mutableStateOf(saved) }
    val focus = LocalFocusManager.current
    val valid = text.isBlank() || customDohUrl(text) != null
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        label = { Text("Адрес DoH") },
        placeholder = { Text("https://example.com/dns-query") },
        supportingText = {
            Text(if (valid) "Только HTTPS. При ошибке используется системный DNS." else "Нужен адрес https://…")
        },
        isError = !valid,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
        keyboardActions =
            KeyboardActions(onDone = {
                if (valid) {
                    onSave(text.trim())
                    focus.clearFocus()
                }
            }),
    )
}

private val VocarooDnsProvider.label: String
    get() =
        when (this) {
            VocarooDnsProvider.SYSTEM -> "Системный"
            VocarooDnsProvider.COMSS -> "Comss"
            VocarooDnsProvider.XBOX -> "XBOX DNS"
            VocarooDnsProvider.GEOHIDE -> "GeoHide"
            VocarooDnsProvider.CUSTOM -> "Свой адрес"
        }

private fun AppSettings.withVocarooDns(transform: (VocarooDns) -> VocarooDns): AppSettings =
    copy(behavior = behavior.copy(vocarooDns = transform(behavior.vocarooDns)))
