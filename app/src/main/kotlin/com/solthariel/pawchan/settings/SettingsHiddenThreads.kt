package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings

internal fun hiddenThreadsSettingsGroup(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): SettingsGroup =
    SettingsGroup(
        "Скрытые треды",
        listOf(
            SettingsItem.Toggle(
                "Выводить скрытые треды",
                "Показывать свёрнутые карточки вместо полного удаления из списка",
                app.behavior.showHiddenThreads,
            ) { enabled ->
                update { current -> current.copy(behavior = current.behavior.copy(showHiddenThreads = enabled)) }
            },
        ),
    )
