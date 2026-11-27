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
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.window.DialogProperties
import com.solthariel.pawchan.core.ui.FullScreenModalWindow
import com.solthariel.pawchan.R.string as L

@Composable
internal fun AboutLicenses() {
    val res = LocalResources.current

    var open by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current
    SettingsItemRow(SettingsItem.Action(res.getString(L.ui_licenses), null) { open = true })
    if (open) LicenseScreen(uriHandler) { open = false }
}

@Composable
private fun LicenseScreen(
    uriHandler: UriHandler,
    close: () -> Unit,
) {
    val res = LocalResources.current

    FullScreenModalWindow(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).systemBarsPadding()) {
            SettingsPageToolbar(res.getString(L.ui_foss_licenses), close) {}
            LicenseInventoryContent(uriHandler)
        }
    }
}
