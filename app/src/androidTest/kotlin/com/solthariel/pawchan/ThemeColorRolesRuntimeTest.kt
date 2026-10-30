package com.solthariel.pawchan

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.ui.PawchanTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThemeColorRolesRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun authorPalettesHaveOwnContainersAndReadableAccentPairs() {
        val samples = mutableMapOf<ThemePreference, ColorScheme>()
        compose.setContent {
            ThemePreference.entries.forEach { preference ->
                PawchanTheme(preference) { samples[preference] = androidx.compose.material3.MaterialTheme.colorScheme }
            }
        }
        compose.runOnIdle {
            val custom = ThemePreference.entries.filter { it !in standard }
            custom.forEach { preference ->
                val scheme = samples.getValue(preference)
                val default = if (preference in dark) darkColorScheme() else lightColorScheme()
                assertNotEquals(preference.name, default.secondaryContainer, scheme.secondaryContainer)
                assertNotEquals(preference.name, default.surfaceTint, scheme.surfaceTint)
                accentPairs(scheme).forEach { (background, foreground) ->
                    assertTrue("$preference: contrast below 4.5", contrast(background, foreground) >= 4.5f)
                }
                assertEquals(scheme.primary, scheme.surfaceTint)
            }
            val amoled = samples.getValue(ThemePreference.AMOLED)
            assertEquals(Color.Black, amoled.background)
            assertEquals(Color.Black, amoled.surface)
            assertEquals(Color.Black, amoled.surfaceContainer)
        }
    }

    private fun accentPairs(scheme: ColorScheme): List<Pair<Color, Color>> =
        listOf(
            scheme.primary to scheme.onPrimary,
            scheme.secondary to scheme.onSecondary,
            scheme.tertiary to scheme.onTertiary,
            scheme.primaryContainer to scheme.onPrimaryContainer,
            scheme.secondaryContainer to scheme.onSecondaryContainer,
            scheme.tertiaryContainer to scheme.onTertiaryContainer,
            scheme.primaryFixed to scheme.onPrimaryFixed,
            scheme.primaryFixedDim to scheme.onPrimaryFixedVariant,
            scheme.secondaryFixed to scheme.onSecondaryFixed,
            scheme.secondaryFixedDim to scheme.onSecondaryFixedVariant,
            scheme.tertiaryFixed to scheme.onTertiaryFixed,
            scheme.tertiaryFixedDim to scheme.onTertiaryFixedVariant,
            scheme.inverseSurface to scheme.inverseOnSurface,
        )

    private fun contrast(
        first: Color,
        second: Color,
    ): Float {
        val a = first.luminance()
        val b = second.luminance()
        return (maxOf(a, b) + 0.05f) / (minOf(a, b) + 0.05f)
    }

    private val standard = setOf(ThemePreference.SYSTEM, ThemePreference.LIGHT, ThemePreference.DARK, ThemePreference.AMOLED)
    private val dark = setOf(ThemePreference.TOMORROW, ThemePreference.HARDON, ThemePreference.NEUTRON)
}
