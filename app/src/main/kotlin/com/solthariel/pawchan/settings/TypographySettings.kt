package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppFont
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.FontEmphasis
import com.solthariel.pawchan.domain.settings.TextLeading
import com.solthariel.pawchan.domain.settings.TypographyPreferences

internal fun typographySettingsGroup(
    app: AppSettings,
    update: ((AppSettings) -> AppSettings) -> Unit,
): SettingsGroup {
    val value = app.behavior.client.reading.typography
    val fonts = AppFont.entries
    val weights = FontEmphasis.entries
    val leading = TextLeading.entries
    return SettingsGroup(
        "Шрифт",
        listOf(
            SettingsItem.Choice(
                "Шрифт приложения",
                listOf("Системный", "Roboto Flex", "Roboto Serif", "Roboto Mono"),
                fonts.indexOf(value.font),
            ) { selected -> update { it.updateTypography { current -> current.copy(font = fonts[selected]) } } },
            SettingsItem.Choice(
                "Насыщенность",
                listOf("Легче", "Обычная", "Плотнее"),
                weights.indexOf(value.emphasis),
            ) { selected -> update { it.updateTypography { current -> current.copy(emphasis = weights[selected]) } } },
            SettingsItem.Choice(
                "Межстрочный интервал",
                listOf("Обычный", "Увеличенный"),
                leading.indexOf(value.leading),
            ) { selected -> update { it.updateTypography { current -> current.copy(leading = leading[selected]) } } },
        ),
    )
}

private fun AppSettings.withTypography(value: TypographyPreferences): AppSettings {
    val client = behavior.client
    return copy(behavior = behavior.copy(client = client.copy(reading = client.reading.copy(typography = value))))
}

private fun AppSettings.updateTypography(transform: (TypographyPreferences) -> TypographyPreferences): AppSettings =
    withTypography(transform(behavior.client.reading.typography))
