package com.solthariel.pawchan.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSelectionModeTest {
    @Test fun blackOverrideMapsOnlySystemAndDarkToAmoled() {
        val colors = ThemeColors(ThemePalette.TEAL, true)
        assertEquals(ThemePreference.AMOLED, ThemePreference.SYSTEM.selectionMode(colors))
        assertEquals(ThemePreference.AMOLED, ThemePreference.DARK.selectionMode(colors))
        assertEquals(ThemePreference.LIGHT, ThemePreference.LIGHT.selectionMode(colors))
        assertEquals(ThemePreference.AMOLED, ThemePreference.AMOLED.selectionMode(ThemeColors()))
    }

    @Test fun rgbSeedAcceptsBothBoundariesAndRejectsAlphaOrNegativeValues() {
        assertEquals(0, ThemeColors(customSeed = 0).customSeed)
        assertEquals(MAX_THEME_SEED, ThemeColors(customSeed = MAX_THEME_SEED).customSeed)
        assertTrue(runCatching { ThemeColors(customSeed = -1) }.exceptionOrNull() is IllegalArgumentException)
        val invalid = runCatching { ThemeColors(customSeed = MAX_THEME_SEED + 1) }
        assertTrue(invalid.exceptionOrNull() is IllegalArgumentException)
    }
}
