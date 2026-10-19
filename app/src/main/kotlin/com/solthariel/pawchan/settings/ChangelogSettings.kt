package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun ChangelogSettings() {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var releases by remember { mutableStateOf<List<ChangelogRelease>?>(null) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(open) {
        if (open && releases == null) {
            failed = false
            releases =
                withContext(Dispatchers.IO) {
                    try {
                        readChangelogHistory(BuildConfig.VERSION_CODE) { path ->
                            context.assets
                                .open(path)
                                .bufferedReader()
                                .use { it.readText() }
                        }
                    } catch (_: IOException) {
                        null
                    } catch (_: IllegalArgumentException) {
                        null
                    }
                }
            failed = releases == null
        }
    }
    SettingsItemRow(SettingsItem.Action("Список изменений", null, onClick = { open = true }))
    if (open) ChangelogDialog(releases, failed) { open = false }
}
