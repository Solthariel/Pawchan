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
import com.solthariel.pawchan.domain.settings.DnsProvider
import com.solthariel.pawchan.domain.settings.ServiceDns
import com.solthariel.pawchan.infrastructure.dns.customDohUrl

/** Независимые настройки внешних сервисов, общий выбор провайдера и редактор URL. */
internal fun vocarooDnsItems(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsItem> =
    serviceDnsItems("Vocaroo", app.behavior.dns.vocaroo) { dns ->
        update { it.copy(behavior = it.behavior.copy(dns = it.behavior.dns.copy(vocaroo = dns))) }
    }

internal fun arhivachDnsItems(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsItem> =
    serviceDnsItems("Архивача", app.behavior.dns.arhivach) { dns ->
        update { it.copy(behavior = it.behavior.copy(dns = it.behavior.dns.copy(arhivach = dns))) }
    }

internal fun githubDnsItems(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsItem> =
    serviceDnsItems("GitHub", app.behavior.dns.github) { dns ->
        update { it.copy(behavior = it.behavior.copy(dns = it.behavior.dns.copy(github = dns))) }
    }

private fun serviceDnsItems(
    service: String,
    dns: ServiceDns,
    onSave: (ServiceDns) -> Unit,
): List<SettingsItem> {
    val providers = DnsProvider.entries
    val choice =
        SettingsItem.Choice(
            "DNS для $service",
            providers.map(DnsProvider::label),
            providers.indexOf(dns.provider),
        ) { index -> onSave(dns.copy(provider = providers[index])) }
    if (dns.provider != DnsProvider.CUSTOM) return listOf(choice)
    return listOf(
        choice,
        SettingsItem.Custom("Адрес DoH для $service") {
            ServiceDohField(dns.customUrl) { url -> onSave(dns.copy(customUrl = url)) }
        },
    )
}

@Composable
private fun ServiceDohField(
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

private val DnsProvider.label: String
    get() =
        when (this) {
            DnsProvider.SYSTEM -> "Системный"
            DnsProvider.COMSS -> "Comss"
            DnsProvider.XBOX -> "XBOX DNS"
            DnsProvider.GEOHIDE -> "GeoHide"
            DnsProvider.CUSTOM -> "Свой адрес"
        }
