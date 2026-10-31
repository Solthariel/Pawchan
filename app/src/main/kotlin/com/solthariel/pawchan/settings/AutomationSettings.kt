package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.AutomationPreferences

internal fun automationGroups(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): List<SettingsGroup> {
    val value = app.behavior.client.automation
    val change: ((AutomationPreferences) -> AutomationPreferences) -> Unit = { transform ->
        update { current -> current.withClient { it.copy(automation = transform(it.automation)) } }
    }
    val minutes = AutomationPreferences.BACKGROUND_INTERVALS
    return listOf(
        foregroundRefreshGroup(value, change),
        SettingsGroup(
            "Фоновая проверка",
            listOf(
                SettingsItem.Toggle(
                    "Обновлять избранное",
                    "Включая отслеживаемые треды",
                    value.favorites,
                ) { on ->
                    change { it.copy(favorites = on) }
                },
                SettingsItem.Toggle(
                    "Проверять ответы мне",
                    "На отслеживаемые посты",
                    value.replies,
                ) { on ->
                    change { it.copy(replies = on) }
                },
                SettingsItem.Choice(
                    "Интервал проверки",
                    minutes.map { "$it минут" },
                    minutes.indexOf(value.backgroundMinutes),
                ) { index ->
                    change { it.copy(backgroundMinutes = minutes[index]) }
                },
                SettingsItem.Toggle(
                    "Проверять по Wi-Fi",
                    "Без мобильного интернета",
                    value.wifiOnly,
                ) { on ->
                    change { it.copy(wifiOnly = on) }
                },
                SettingsItem.Custom("Уведомления об ответах") { NotificationSettings(value, change) },
            ),
        ),
    )
}

private fun foregroundRefreshGroup(
    value: AutomationPreferences,
    change: ((AutomationPreferences) -> AutomationPreferences) -> Unit,
): SettingsGroup {
    val seconds = AutomationPreferences.OPEN_INTERVALS
    return SettingsGroup(
        "Открытый тред",
        listOf(
            SettingsItem.Toggle(
                "Обновлять открытый тред",
                null,
                value.openThread,
            ) { on ->
                change { it.copy(openThread = on) }
            },
            SettingsItem.Choice(
                "Интервал обновления",
                seconds.map { "$it секунд" },
                seconds.indexOf(value.openIntervalSeconds),
            ) { index ->
                change { it.copy(openIntervalSeconds = seconds[index]) }
            },
        ),
    )
}
