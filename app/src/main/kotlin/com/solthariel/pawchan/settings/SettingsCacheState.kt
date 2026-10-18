package com.solthariel.pawchan.settings

import android.content.Context
import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Размер и очистка кэша приложения (миниатюры, видео, временные файлы). */
internal class SettingsCacheState(
    private val context: Context,
    private val loaders: com.solthariel.pawchan.infrastructure.MediaLoaders?,
) {
    var size by mutableStateOf<String?>(null)
        private set

    suspend fun refresh() {
        val bytes =
            withContext(Dispatchers.IO) {
                context.cacheDir
                    .walkBottomUp()
                    .filter(File::isFile)
                    .sumOf(File::length)
            }
        size = Formatter.formatShortFileSize(context, bytes)
    }

    suspend fun clear() {
        size = null
        loaders?.thumbnailLoader?.clearCache()
        loaders?.imageLoader?.memoryCache?.clear()
        withContext(Dispatchers.IO) { loaders?.imageLoader?.diskCache?.clear() }
        refresh()
    }
}

@Composable
internal fun rememberSettingsEnvironment(updates: @Composable () -> Unit): SettingsEnvironment {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val loaders =
        com.solthariel.pawchan.workspace.LocalWorkspace.current
            ?.loaders
    val cache = remember(context, loaders) { SettingsCacheState(context.applicationContext, loaders) }
    var confirm by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(cache) { cache.refresh() }
    if (confirm) {
        CacheClearDialog({ confirm = false }) {
            confirm = false
            scope.launch {
                try {
                    cache.clear()
                } catch (_: java.io.IOException) {
                    failure = "Не удалось полностью очистить кэш"
                }
            }
        }
    }
    failure?.let { text -> CacheNotice(text) { failure = null } }
    return SettingsEnvironment(cache.size, { confirm = true }, updates)
}
