package com.solthariel.pawchan

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.ui.presetColorScheme
import com.solthariel.pawchan.ui.withPureBlackSurfaces
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorContrastTest {
    @Test fun presetTextRolesHaveReadableContrast() {
        ThemePalette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                assertReadable(presetColorScheme(palette, dark), "$palette dark=$dark")
            }
        }
    }

    @Test fun blackSurfaceOverrideKeepsTextReadable() {
        ThemePalette.entries.forEach { palette ->
            assertReadable(presetColorScheme(palette, true).withPureBlackSurfaces(), "$palette black")
        }
    }

    private fun assertReadable(
        colors: ColorScheme,
        label: String,
    ) {
        val pairs =
            listOf(
                colors.primary to colors.onPrimary,
                colors.primaryContainer to colors.onPrimaryContainer,
                colors.secondary to colors.onSecondary,
                colors.secondaryContainer to colors.onSecondaryContainer,
                colors.tertiary to colors.onTertiary,
                colors.tertiaryContainer to colors.onTertiaryContainer,
                colors.error to colors.onError,
                colors.errorContainer to colors.onErrorContainer,
                colors.background to colors.onBackground,
                colors.surfaceVariant to colors.onSurfaceVariant,
            ) +
                listOf(
                    colors.surface,
                    colors.surfaceDim,
                    colors.surfaceBright,
                    colors.surfaceContainerLowest,
                    colors.surfaceContainerLow,
                    colors.surfaceContainer,
                    colors.surfaceContainerHigh,
                    colors.surfaceContainerHighest,
                ).map { it to colors.onSurface }
        pairs.forEachIndexed { index, (background, text) ->
            val ratio = contrast(background, text)
            assertTrue("$label pair=$index contrast=$ratio", ratio >= MIN_TEXT_CONTRAST)
        }
    }

    private fun contrast(
        first: Color,
        second: Color,
    ): Float {
        val light = maxOf(first.luminance(), second.luminance())
        val dark = minOf(first.luminance(), second.luminance())
        return (light + LUMINANCE_OFFSET) / (dark + LUMINANCE_OFFSET)
    }
}

private const val MIN_TEXT_CONTRAST = 4.5f
private const val LUMINANCE_OFFSET = 0.05f
