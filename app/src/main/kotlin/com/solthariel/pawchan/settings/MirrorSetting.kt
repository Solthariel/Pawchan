package com.solthariel.pawchan.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.validMirrorHost

@Composable
internal fun MirrorSetting(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
) {
    val mirrors = com.solthariel.pawchan.infrastructure.LocalForumMirrors.current
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
                mirrors?.selectHost(options[index])
                update { it.withClient { c -> c.copy(connection = c.connection.copy(preferredHost = options[index])) } }
            }
        },
    )
    if (mirrors != null) {
        val state by mirrors.state.collectAsStateWithLifecycle()
        activeMirrorCaption(host, state.activeHost)?.let { caption ->
            Text(
                caption,
                Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    MirrorAvailabilitySetting(app, update)
    if (custom) {
        CustomMirrorDialog(input, { input = it }, { custom = false }) {
            mirrors?.selectHost(input)
            update { it.withClient { c -> c.copy(connection = c.connection.copy(preferredHost = input)) } }
            custom = false
        }
    }
}

private const val CUSTOM_MIRROR_INDEX = 3

private val DEFAULT_MIRRORS = setOf("2ch.su", "2ch.org", "2ch.life")

@Composable
private fun CustomMirrorDialog(
    input: String,
    onInput: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onClose,
        title = { Text("Свой домен") },
        text = {
            OutlinedTextField(
                input,
                { onInput(it.trim().lowercase()) },
                label = { Text("Домен без https://") },
                singleLine = true,
                isError = !validMirrorHost(input),
            )
        },
        confirmButton = {
            TextButton(enabled = validMirrorHost(input), onClick = onSave) { Text("Сохранить") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("Отмена") } },
    )
}
