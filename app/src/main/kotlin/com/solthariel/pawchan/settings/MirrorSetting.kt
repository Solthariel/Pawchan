package com.solthariel.pawchan.settings

import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.validMirrorHost

@Composable
internal fun MirrorSetting(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
) {
    val host = app.behavior.client.mirror
    val options =
        listOf(
            "2ch.su",
            "2ch.org",
            "2ch.life",
            if (host in DEFAULT_MIRRORS) "Свой домен" else host,
        )
    var custom by remember { mutableStateOf(false) }
    var input by remember(host) { mutableStateOf(host) }
    SettingsItemRow(
        SettingsItem.Choice(
            "Зеркало сайта",
            options,
            options.indexOf(host).takeIf { it >= 0 } ?: CUSTOM_MIRROR_INDEX,
        ) { index ->
            if (index == CUSTOM_MIRROR_INDEX) {
                custom = true
            } else {
                update { it.withClient { c -> c.copy(mirror = options[index]) } }
            }
        },
    )
    if (custom) {
        AppAlertDialog(
            onDismissRequest = { custom = false },
            title = { Text("Свой домен") },
            text = {
                OutlinedTextField(
                    input,
                    { input = it.trim().lowercase() },
                    label = { Text("Домен без https://") },
                    singleLine = true,
                    isError = !validMirrorHost(input),
                )
            },
            confirmButton = {
                TextButton(enabled = validMirrorHost(input), onClick = {
                    update { it.withClient { c -> c.copy(mirror = input) } }
                    custom = false
                }) { Text("Сохранить") }
            },
            dismissButton = { TextButton(onClick = { custom = false }) { Text("Отмена") } },
        )
    }
}

private const val CUSTOM_MIRROR_INDEX = 3

private val DEFAULT_MIRRORS = setOf("2ch.su", "2ch.org", "2ch.life")
