package com.solthariel.pawchan.thread.watch

import com.solthariel.pawchan.domain.settings.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Same controller owns lifecycle, avoiding a second polling loop. */
internal fun bindAutomaticThreadRefresh(
    scope: CoroutineScope,
    settings: AppSettingsRepository?,
    watcher: ThreadWatcherController,
) = scope.launch {
    settings?.appSettings?.map { it.behavior.client.automation }?.distinctUntilChanged()?.collectLatest { prefs ->
        watcher.configureAutomatic(prefs.openThread, prefs.openIntervalSeconds * MILLIS_PER_SECOND)
    }
}

private const val MILLIS_PER_SECOND = 1000L
