package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.domain.settings.selectionMode

internal fun themeSettingsGroup(
    state: SettingsUiState,
    actions: SettingsActions,
): SettingsGroup {
    val modes = listOf(ThemePreference.SYSTEM, ThemePreference.LIGHT, ThemePreference.DARK, ThemePreference.AMOLED)
    return SettingsGroup(
        "Оформление",
        listOf(
            SettingsItem.Choice(
                "Режим темы",
                listOf("Как в системе", "Светлый", "Тёмный", "AMOLED"),
                modes.indexOf(state.theme.selectionMode(state.themeColors)),
            ) { actions.onTheme(modes[it]) },
            SettingsItem.Custom("Цветовая палитра") {
                ThemePaletteRow(state, actions.onThemeColors)
            },
        ),
    )
}
