package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import com.solthariel.pawchan.BuildConfig
import com.solthariel.pawchan.core.ui.AppAlertDialog

internal const val RELEASE_URL = "https://github.com/Solthariel/Pawchan/releases/latest"
internal const val AUTHOR_URL = "https://solthariel.github.io/"

@Composable
internal fun AboutLink(
    title: String,
    summary: String,
    url: String,
) {
    val uri = LocalUriHandler.current
    SettingsItemRow(SettingsItem.Action(title, summary, onClick = { uri.openUri(url) }))
}

@Composable
internal fun AboutVersions() {
    var open by remember { mutableStateOf(false) }
    SettingsItemRow(SettingsItem.Action("Версии компонентов", "Инструменты и основные библиотеки") { open = true })
    if (open) {
        AppAlertDialog(
            onDismissRequest = { open = false },
            title = { Text("Версии компонентов") },
            text = { Text(BuildConfig.COMPONENT_VERSIONS) },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Закрыть") } },
        )
    }
}
