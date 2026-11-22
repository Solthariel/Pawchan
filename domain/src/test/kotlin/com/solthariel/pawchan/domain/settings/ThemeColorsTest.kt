package com.solthariel.pawchan.domain.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ThemeColorsTest {
    @Test fun newInstallUsesWallpaperWithoutBlackOverride() {
        assertEquals(ThemePalette.SYSTEM, ThemeColors().palette)
        assertFalse(ThemeColors().pureBlack)
    }

    @Test fun legacyLightThemesKeepLightMode() {
        listOf(ThemePreference.LIGHT, ThemePreference.NORMIE, ThemePreference.BURICHAN, ThemePreference.PHOTON)
            .forEach { assertEquals(ThemePreference.LIGHT, it.displayMode()) }
    }

    @Test fun legacyDarkThemesKeepDarkMode() {
        listOf(
            ThemePreference.DARK,
            ThemePreference.AMOLED,
            ThemePreference.TOMORROW,
            ThemePreference.HARDON,
            ThemePreference.NEUTRON,
        ).forEach { assertEquals(ThemePreference.DARK, it.displayMode()) }
        assertEquals(ThemePreference.SYSTEM, ThemePreference.SYSTEM.displayMode())
    }

    @Test fun onlyLegacyAmoledEnablesBlackByDefault() {
        ThemePreference.entries.forEach {
            assertEquals(it == ThemePreference.AMOLED, it.legacyColors().pureBlack)
        }
    }

    @Test fun replacedThemesUseDocumentedClosestPreset() {
        assertEquals(ThemePalette.BLUE, ThemePreference.TOMORROW.legacyColors().palette)
        assertEquals(ThemePalette.BLUE, ThemePreference.NORMIE.legacyColors().palette)
        assertEquals(ThemePalette.LAVENDER, ThemePreference.BURICHAN.legacyColors().palette)
        assertEquals(ThemePalette.PEACH, ThemePreference.HARDON.legacyColors().palette)
        assertEquals(ThemePalette.BLUE, ThemePreference.NEUTRON.legacyColors().palette)
        assertEquals(ThemePalette.PEACH, ThemePreference.PHOTON.legacyColors().palette)
    }
}
