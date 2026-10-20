package com.solthariel.pawchan.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.BuildConfig
import java.io.File

@Composable
internal fun ChangelogSettings() {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    var releases by remember { mutableStateOf<List<ChangelogRelease>?>(null) }
    var failed by remember { mutableStateOf(false) }
    val repository =
        remember(context) {
            val url = historyUrl(BuildConfig.UPDATE_MANIFEST_URL)
            val http = ChangelogHttp()
            ChangelogHistoryRepository(
                cache = url?.let { ChangelogCache(File(context.filesDir, "changelog/history-v1.json"), it) },
                bundled = {
                    readChangelogHistory(BuildConfig.VERSION_CODE) { path ->
                        context.assets
                            .open(path)
                            .bufferedReader()
                            .use { it.readText() }
                    }
                },
                download = { etag -> url?.let { http.download(it, etag) } ?: ChangelogDownload(null, null) },
            )
        }
    LaunchedEffect(open) {
        if (open) {
            failed = false
            try {
                releases = repository.local()
                repository.refresh()?.let { releases = it }
            } catch (_: java.io.IOException) {
                failed = releases == null
            } catch (_: IllegalArgumentException) {
                failed = releases == null
            }
        }
    }
    SettingsItemRow(SettingsItem.Action("Список изменений", null, onClick = { open = true }))
    if (open) ChangelogDialog(releases, failed) { open = false }
}
