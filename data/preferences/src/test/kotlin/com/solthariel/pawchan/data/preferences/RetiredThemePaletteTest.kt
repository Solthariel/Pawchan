package com.solthariel.pawchan.data.preferences

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.solthariel.pawchan.domain.settings.ThemeColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import org.junit.Assert.assertEquals
import org.junit.Test

class RetiredThemePaletteTest {
    @Test fun oldOliveSelectionBecomesVanillaWithoutLosingBlackOrCustomSeed() {
        assertRetiredPalette("OLIVE", ThemePalette.AMBER)
    }

    @Test fun oldPowderSelectionBecomesCoralWithoutLosingBlackOrCustomSeed() {
        assertRetiredPalette("ROSE", ThemePalette.BURGUNDY)
    }

    @Test fun savingRetiredKeysWritesOnlyTheirActiveReplacements() {
        val values = mutablePreferencesOf()
        values.writeThemeColors(ThemeColors(ThemePalette.OLIVE, true, TEST_SEED))
        assertEquals("AMBER", values[stringPreferencesKey("theme_palette")])
        values.writeThemeColors(ThemeColors(ThemePalette.ROSE, true, TEST_SEED))
        assertEquals("BURGUNDY", values[stringPreferencesKey("theme_palette")])
        assertEquals(ThemeColors(ThemePalette.BURGUNDY, true, TEST_SEED), values.readThemeColors())
    }

    @Test fun activePalettesRemainUnchanged() {
        listOf(
            ThemePalette.LAVENDER,
            ThemePalette.BLUE,
            ThemePalette.PEACH,
            ThemePalette.BURGUNDY,
            ThemePalette.AMBER,
            ThemePalette.GREEN,
            ThemePalette.TEAL,
            ThemePalette.GRAY,
            ThemePalette.CUSTOM,
            ThemePalette.SYSTEM,
        ).forEach { palette ->
            val values = mutablePreferencesOf()
            val expected = ThemeColors(palette, true, TEST_SEED)
            values.writeThemeColors(expected)
            assertEquals(expected, values.readThemeColors())
        }
    }

    private fun assertRetiredPalette(
        old: String,
        expected: ThemePalette,
    ) {
        val values =
            mutablePreferencesOf(
                stringPreferencesKey("theme_palette") to old,
                stringPreferencesKey("theme") to "AMOLED",
                booleanPreferencesKey("theme_pure_black") to true,
                intPreferencesKey("theme_custom_seed") to TEST_SEED,
            )
        assertEquals(ThemeColors(expected, true, TEST_SEED), values.readThemeColors())
        assertEquals("AMOLED", values[stringPreferencesKey("theme")])
    }
}

private const val TEST_SEED = 0x123456
