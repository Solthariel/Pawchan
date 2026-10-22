package com.solthariel.pawchan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.solthariel.pawchan.settings.ChangelogDownload
import com.solthariel.pawchan.settings.ChangelogHistoryController
import com.solthariel.pawchan.settings.ChangelogHistoryRepository
import com.solthariel.pawchan.settings.readChangelogHistory

/** UI geometry/history fixtures use real bundled assets, never a live GitHub request. */
@Composable
internal fun rememberBundledTestChangelogHistory(): ChangelogHistoryController {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context, scope) {
        val repository =
            ChangelogHistoryRepository(
                cache = null,
                bundled = {
                    readChangelogHistory(BuildConfig.VERSION_CODE) { path ->
                        context.assets
                            .open(path)
                            .bufferedReader()
                            .use { it.readText() }
                    }
                },
                download = { _, _ -> ChangelogDownload(null, null) },
            )
        ChangelogHistoryController(repository, scope)
    }
}
