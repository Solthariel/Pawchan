package com.solthariel.pawchan.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.FullScreenModalWindow

@Composable
internal fun AboutLicenses() {
    var open by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    SettingsItemRow(SettingsItem.Action("Лицензии", null) { open = true })
    if (open) LicenseScreen(uriHandler) { open = false }
}

@Composable
private fun LicenseScreen(
    uriHandler: UriHandler,
    close: () -> Unit,
) {
    FullScreenModalWindow(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
            SettingsPageToolbar("Лицензии FOSS", close) {}
            LicenseInventoryContent(uriHandler)
        }
    }
}
