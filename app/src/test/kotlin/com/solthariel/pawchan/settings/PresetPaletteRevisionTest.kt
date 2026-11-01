package com.solthariel.pawchan.settings

import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.ui.presetColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetPaletteRevisionTest {
    @Test fun eightPresetsAreAvailableWhileRetiredKeysRemainReadable() {
        val names =
            listOf(
                "Лавандовая",
                "Васильковая",
                "Персиковая",
                "Коралловая",
                "Ванильная",
                "Мятная",
                "Бирюзовая",
                "Галечная",
            )
        val presets = availableThemePalettes(30).filter { it != ThemePalette.CUSTOM }
        assertEquals(names, presets.map { it.paletteLabel() })
        assertEquals(ThemePalette.ROSE, ThemePalette.valueOf("ROSE"))
        assertEquals(ThemePalette.AMBER, ThemePalette.valueOf("AMBER"))
        assertEquals(ThemePalette.GREEN, ThemePalette.valueOf("GREEN"))
    }

    @Test fun mintAndTurquoiseAreDistinctGreenAndCyanNotYellowGreen() {
        val mint = containerHct(ThemePalette.GREEN)
        val turquoise = containerHct(ThemePalette.TEAL)
        assertTrue("Mint hue=${mint.hue}", mint.hue in 150.0..175.0)
        assertTrue("Turquoise hue=${turquoise.hue}", turquoise.hue in 185.0..210.0)
        assertTrue(turquoise.hue - mint.hue > MIN_HUE_SEPARATION)
        assertTrue(mint.chroma < MAX_SOFT_CHROMA && turquoise.chroma < MAX_SOFT_CHROMA)
    }

    @Test fun vanillaIsYellowAndCoralRemainsWarm() {
        assertTrue(containerHct(ThemePalette.AMBER).hue in 85.0..110.0)
        assertTrue(containerHct(ThemePalette.BURGUNDY).hue in 10.0..35.0)
    }

    @Test fun mutedPresetsKeepLowChromaInLightAndDarkModes() {
        listOf(ThemePalette.GREEN, ThemePalette.TEAL, ThemePalette.AMBER)
            .forEach { palette ->
                listOf(false, true).forEach { dark ->
                    val scheme = presetColorScheme(palette, dark)
                    val accent = Hct.fromInt(scheme.primary.toArgb())
                    assertTrue("$palette dark=$dark chroma=${accent.chroma}", accent.chroma < MAX_SOFT_CHROMA)
                }
            }
    }

    @Test fun pebbleIsNeutralWithoutPureBlackOrWhitePrimary() {
        listOf(false, true).forEach { dark ->
            val primary = presetColorScheme(ThemePalette.GRAY, dark).primary.toArgb()
            assertTrue(primary != OPAQUE_BLACK && primary != OPAQUE_WHITE)
            assertTrue(Hct.fromInt(primary).chroma < MAX_NEUTRAL_CHROMA)
        }
    }

    @Test fun retiredPresetsResolveToTheirReplacementInsteadOfAnotherHiddenTheme() {
        listOf(ThemePalette.OLIVE to ThemePalette.AMBER, ThemePalette.ROSE to ThemePalette.BURGUNDY)
            .forEach { (old, replacement) ->
                listOf(false, true).forEach { dark ->
                    val previous = presetColorScheme(old, dark)
                    val active = presetColorScheme(replacement, dark)
                    assertEquals(active.primary, previous.primary)
                    assertEquals(active.primaryContainer, previous.primaryContainer)
                    assertEquals(active.surface, previous.surface)
                }
            }
    }

    private fun containerHct(palette: ThemePalette): Hct {
        val colors = presetColorScheme(palette, false)
        return Hct.fromInt(colors.primaryContainer.toArgb())
    }
}

private const val MIN_HUE_SEPARATION = 20.0
private const val MAX_SOFT_CHROMA = 27.0
private const val MAX_NEUTRAL_CHROMA = 4.0
private const val OPAQUE_BLACK = -0x1000000
private const val OPAQUE_WHITE = -1
