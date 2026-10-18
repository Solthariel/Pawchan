package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings

internal fun storageGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
    environment: SettingsEnvironment,
): List<SettingsGroup> =
    listOf(
        SettingsGroup(
            "Кэш",
            listOf(
                cacheLimitItem(app, update),
                SettingsItem.Action(
                    environment.cacheSize?.let { "Очистить кэш — $it" } ?: "Очистить кэш",
                    null,
                    true,
                    environment.onClearCache,
                ),
                SettingsItem.Toggle(
                    "Очищать кэш изображений при выходе",
                    "При закрытии приложения, не при сворачивании",
                    app.behavior.client.media.clearCacheOnExit,
                ) { on ->
                    update { current -> current.withClient { it.copy(media = it.media.copy(clearCacheOnExit = on)) } }
                },
            ),
        ),
    )
