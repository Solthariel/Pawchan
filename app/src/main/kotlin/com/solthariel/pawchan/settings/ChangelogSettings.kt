package com.solthariel.pawchan.settings

import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.solthariel.pawchan.BuildConfig
import com.solthariel.pawchan.core.ui.UiForwardIndicator
import java.io.File

@Composable
internal fun ChangelogSettings(
    history: ChangelogHistoryController = rememberChangelogHistory(),
    showVersion: Boolean = false,
) {
    var open by remember { mutableStateOf(false) }
    val state by history.state.collectAsState()
    LaunchedEffect(open) {
        if (open) history.open()
    }
    if (showVersion) {
        ListItem(
            headlineContent = {
                val secondary = MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    buildAnnotatedString {
                        append("Версия ")
                        withStyle(SpanStyle(color = secondary)) { append(BuildConfig.VERSION_NAME) }
                    },
                    Modifier.testTag("about-version"),
                )
            },
            trailingContent = {
                TextButton(onClick = { open = true }) {
                    Text("Список изменений")
                    UiForwardIndicator()
                }
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        )
    } else {
        SettingsItemRow(SettingsItem.Action("Список изменений", null, onClick = { open = true }))
    }
    if (open) ChangelogDialog(state.releases, state.failed) { open = false }
}

@Composable
internal fun rememberChangelogHistory(): ChangelogHistoryController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val network = com.solthariel.pawchan.infrastructure.LocalGithubNetwork.current
    return remember(context, scope, network) {
        val url = historyUrl(BuildConfig.UPDATE_MANIFEST_URL)
        val http = ChangelogHttp(network.historyClient)
        val repository =
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
                download = { etag, minimumCode ->
                    url?.let { http.download(it, etag, minimumCode) } ?: ChangelogDownload(null, null)
                },
            )
        ChangelogHistoryController(repository, scope)
    }
}
