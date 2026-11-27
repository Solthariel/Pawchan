package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalUriHandler
import com.solthariel.pawchan.core.ui.AppAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import com.solthariel.pawchan.R.string as L

/** A single bundled document; no network is needed to read help. */
@Composable
internal fun AutohideHelpDialog(close: () -> Unit) {
    val res = LocalResources.current

    val assets = LocalContext.current.applicationContext.assets
    val text by produceState(res.getString(L.ui_loading_help), assets) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    assets.open("AUTOHIDE.md").bufferedReader().use { it.readText() }
                } catch (_: IOException) {
                    res.getString(L.ui_failed_to_open_help_the)
                }
            }
    }
    val uri = LocalUriHandler.current
    AppAlertDialog(
        onDismissRequest = close,
        title = { Text(res.getString(L.ui_how_auto_hide_works)) },
        // AppAlertDialog owns the bounded scrolling body.
        text = { Text(text) },
        confirmButton = { TextButton(onClick = close) { Text(res.getString(L.ui_close)) } },
        dismissButton = {
            TextButton(onClick = { uri.openUri(AUTOHIDE_HELP_URL) }) { Text(res.getString(L.ui_open_on_github)) }
        },
    )
}

// The bundled document is authoritative for the installed version. This link opens the latest source document.
private const val AUTOHIDE_HELP_URL = "https://github.com/Solthariel/Pawchan/blob/master/docs/AUTOHIDE.md"
