package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.settings.DEFAULT_THEME_SEED
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.ui.seedColor
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomColorInitialSeedTest {
    @Test
    fun everyPresetUsesItsOwnSeedInsteadOfPreviouslySavedCustomColor() {
        ThemePalette.entries.filter { it != ThemePalette.SYSTEM && it != ThemePalette.CUSTOM }.forEach { palette ->
            val value = initialCustomSeed(ThemeColors(palette = palette, customSeed = 0x23A080), null)
            assertEquals(palette.seedColor(), value)
        }
        assertEquals(0x6750A4, initialCustomSeed(ThemeColors(palette = ThemePalette.LAVENDER), null))
        assertEquals(0xB85C26, initialCustomSeed(ThemeColors(palette = ThemePalette.PEACH), null))
    }

    @Test
    fun customPaletteReopensItsSavedSeed() {
        val colors = ThemeColors(palette = ThemePalette.CUSTOM, customSeed = 0x23A080)
        assertEquals(0x23A080, initialCustomSeed(colors, null))
    }

    @Test
    fun wallpaperUsesCurrentAccentAndOldDevicesUseLavenderFallback() {
        val colors = ThemeColors(palette = ThemePalette.SYSTEM)
        assertEquals(0x3482A0, initialCustomSeed(colors, 0xFF3482A0.toInt()))
        assertEquals(DEFAULT_THEME_SEED, initialCustomSeed(colors, null))
    }
}
