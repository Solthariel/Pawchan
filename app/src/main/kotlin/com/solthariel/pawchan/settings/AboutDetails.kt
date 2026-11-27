package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import com.solthariel.pawchan.BuildConfig
import com.solthariel.pawchan.core.ui.AppAlertDialog
import com.solthariel.pawchan.R.string as L

internal const val RELEASE_URL = "https://github.com/Solthariel/Pawchan/releases/latest"

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
    val res = LocalResources.current

    var open by remember { mutableStateOf(false) }
    SettingsItemRow(
        SettingsItem.Action(res.getString(L.ui_component_versions), res.getString(L.ui_tools_and_core_libraries)) {
            open =
                true
        },
    )
    if (open) {
        AppAlertDialog(
            onDismissRequest = { open = false },
            title = { Text(res.getString(L.ui_component_versions)) },
            text = { Text(BuildConfig.COMPONENT_VERSIONS) },
            confirmButton = { TextButton(onClick = { open = false }) { Text(res.getString(L.ui_close)) } },
        )
    }
}
