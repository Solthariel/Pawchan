package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.AppFont
import com.solthariel.pawchan.domain.settings.AppSettings
import com.solthariel.pawchan.domain.settings.FontEmphasis
import com.solthariel.pawchan.domain.settings.TextLeading
import com.solthariel.pawchan.domain.settings.TypographyPreferences
import org.junit.Assert.assertEquals
import org.junit.Test

class TypographySettingsTest {
    @Test fun groupContainsOnlyTheThreeFontChoices() {
        val group = typographySettingsGroup(AppSettings()) { }
        assertEquals("Шрифт", group.title)
        assertEquals(
            listOf("Шрифт приложения", "Насыщенность", "Межстрочный интервал"),
            group.items.map { it.title },
        )
        assertEquals(listOf(0, 1, 0), group.items.map { (it as SettingsItem.Choice).selected })
    }

    @Test fun choicesUpdateCurrentSettingsWithoutResettingOtherValues() {
        var current = AppSettings()
        val group = typographySettingsGroup(current) { transform -> current = transform(current) }
        val choices = group.items.map { it as SettingsItem.Choice }
        current = current.copy(textSize = 16)
        choices[0].onSelect(2)
        choices[1].onSelect(2)
        choices[2].onSelect(1)
        assertEquals(16, current.textSize)
        assertEquals(
            TypographyPreferences(AppFont.ROBOTO_SERIF, FontEmphasis.STRONG, TextLeading.RELAXED),
            current.behavior.client.reading.typography,
        )
        val updated = typographySettingsGroup(current) { transform -> current = transform(current) }
        assertEquals(listOf(2, 2, 1), updated.items.map { (it as SettingsItem.Choice).selected })
    }
}
