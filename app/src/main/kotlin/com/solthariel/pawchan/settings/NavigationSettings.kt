package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings

internal fun generalNavigationGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> =
    listOf(
        SettingsGroup("Подключение", listOf(SettingsItem.Custom("Зеркало сайта") { MirrorSetting(app, update) })),
    )
