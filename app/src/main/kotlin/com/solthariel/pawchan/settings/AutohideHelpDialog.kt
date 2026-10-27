package com.solthariel.pawchan.settings

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import com.solthariel.pawchan.core.ui.AppAlertDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** A single bundled document; no network is needed to read help. */
@Composable
internal fun AutohideHelpDialog(close: () -> Unit) {
    val assets = LocalContext.current.applicationContext.assets
    val text by produceState("Загрузка справки…", assets) {
        value =
            withContext(Dispatchers.IO) {
                try {
                    assets.open("AUTOHIDE.md").bufferedReader().use { it.readText() }
                } catch (_: IOException) {
                    "Не удалось открыть справку. Документ docs/AUTOHIDE.md есть в архиве проекта."
                }
            }
    }
    val uri = LocalUriHandler.current
    AppAlertDialog(
        onDismissRequest = close,
        title = { Text("Как работает автоскрытие") },
        // AppAlertDialog owns the bounded scrolling body.
        text = { Text(text) },
        confirmButton = { TextButton(onClick = close) { Text("Закрыть") } },
        dismissButton = {
            TextButton(onClick = { uri.openUri(AUTOHIDE_HELP_URL) }) { Text("Открыть на GitHub") }
        },
    )
}

// The bundled document is authoritative for the installed version. This link opens the latest source document.
private const val AUTOHIDE_HELP_URL = "https://github.com/Solthariel/Pawchan/blob/master/docs/AUTOHIDE.md"
