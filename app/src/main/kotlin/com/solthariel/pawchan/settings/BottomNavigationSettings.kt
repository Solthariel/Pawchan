package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.NavigationPreferences

internal fun bottomNavigationSettingsGroup(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): SettingsGroup =
    SettingsGroup(
        "Нижняя панель",
        listOf(
            SettingsItem.Scale(
                "Прозрачность нижней панели",
                app.behavior.client.reading.navigation
                    .normalized()
                    .bottomBarTransparencyPercent,
                0..NavigationPreferences.MAX_TRANSPARENCY_PERCENT,
                "%",
                preview = false,
            ) { percent ->
                update { current ->
                    current.withClient { client ->
                        client.copy(
                            reading =
                                client.reading.copy(
                                    navigation =
                                        client.reading.navigation
                                            .copy(bottomBarTransparencyPercent = percent)
                                            .normalized(),
                                ),
                        )
                    }
                }
            },
        ),
    )
