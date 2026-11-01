package com.solthariel.pawchan.settings

import androidx.compose.material3.ColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.solthariel.pawchan.core.ui.postTextColors
import com.solthariel.pawchan.domain.settings.ThemePalette
import com.solthariel.pawchan.ui.generatedColorScheme
import com.solthariel.pawchan.ui.presetColorScheme
import com.solthariel.pawchan.ui.withPureBlackSurfaces
import org.junit.Assert.assertTrue
import org.junit.Test

/** Role relationships only: not a replacement for screenshots or Android accessibility checks. */
class ColorRoleMatrixTest {
    @Test fun presetsKeepSecondaryTextReadableOnEverySurface() {
        ThemePalette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                assertSurfaceText(presetColorScheme(palette, dark))
            }
            assertSurfaceText(presetColorScheme(palette, true).withPureBlackSurfaces())
        }
    }

    @Test fun customSeedSampleKeepsSurfaceAndInverseTextReadable() {
        repeat(SEED_SAMPLES) { index ->
            val seed = (index * SEED_STEP) and RGB_MASK
            listOf(false, true).forEach { dark ->
                val scheme = generatedColorScheme(seed, dark)
                assertSurfaceText(scheme)
                assertContrast(scheme.inverseSurface, scheme.inverseOnSurface)
                assertContrast(scheme.inverseSurface, scheme.inversePrimary)
            }
        }
    }

    @Test fun fixedAccentsHaveReadablePairedForegroundsInBothModes() {
        ThemePalette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val colors = presetColorScheme(palette, dark)
                fixedPairs(colors).forEach { (fill, text) -> assertContrast(fill, text) }
            }
        }
    }

    @Test fun menuScrollbarRoleHasNonTextContrastAgainstMenuSurface() {
        ThemePalette.entries.forEach { palette ->
            listOf(false, true).forEach { dark ->
                val colors = presetColorScheme(palette, dark)
                assertContrast(colors.surfaceContainer, colors.outline, MIN_GRAPHIC_CONTRAST)
            }
        }
    }

    private fun assertSurfaceText(colors: ColorScheme) {
        val semantic = postTextColors(colors.surface)
        listOf(
            colors.background,
            colors.surface,
            colors.surfaceVariant,
            colors.surfaceDim,
            colors.surfaceBright,
            colors.surfaceContainerLowest,
            colors.surfaceContainerLow,
            colors.surfaceContainer,
            colors.surfaceContainerHigh,
            colors.surfaceContainerHighest,
        ).forEach { background ->
            assertContrast(background, colors.onSurface)
            assertContrast(background, colors.onSurfaceVariant)
            assertContrast(background, semantic.link)
            assertContrast(background, semantic.quote)
            assertContrast(background, semantic.red)
        }
    }

    private fun fixedPairs(colors: ColorScheme): List<Pair<Color, Color>> =
        listOf(
            colors.primaryFixed to colors.onPrimaryFixed,
            colors.primaryFixed to colors.onPrimaryFixedVariant,
            colors.primaryFixedDim to colors.onPrimaryFixed,
            colors.primaryFixedDim to colors.onPrimaryFixedVariant,
            colors.secondaryFixed to colors.onSecondaryFixed,
            colors.secondaryFixed to colors.onSecondaryFixedVariant,
            colors.secondaryFixedDim to colors.onSecondaryFixed,
            colors.secondaryFixedDim to colors.onSecondaryFixedVariant,
            colors.tertiaryFixed to colors.onTertiaryFixed,
            colors.tertiaryFixed to colors.onTertiaryFixedVariant,
            colors.tertiaryFixedDim to colors.onTertiaryFixed,
            colors.tertiaryFixedDim to colors.onTertiaryFixedVariant,
        )

    private fun assertContrast(
        background: Color,
        text: Color,
        minimum: Float = MIN_TEXT_CONTRAST,
    ) {
        val bright = maxOf(background.luminance(), text.luminance())
        val dark = minOf(background.luminance(), text.luminance())
        val ratio = (bright + LUMINANCE_OFFSET) / (dark + LUMINANCE_OFFSET)
        assertTrue("Background=$background text=$text contrast=$ratio", ratio >= minimum)
    }
}

private const val SEED_SAMPLES = 32
private const val SEED_STEP = 524287
private const val RGB_MASK = 0xFFFFFF
private const val LUMINANCE_OFFSET = 0.05f
private const val MIN_TEXT_CONTRAST = 4.5f
private const val MIN_GRAPHIC_CONTRAST = 3f
