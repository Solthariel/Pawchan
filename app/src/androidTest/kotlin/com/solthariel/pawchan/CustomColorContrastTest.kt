package com.solthariel.pawchan

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.solthariel.pawchan.ui.generatedColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomColorContrastTest {
    @Test fun customSeedsHaveReadablePrimaryAndSurfacePairsInBothModes() {
        listOf(0, RGB_WHITE, RGB_RED, RGB_GREEN, RGB_BLUE, RGB_CUSTOM).forEach { seed ->
            listOf(false, true).forEach { dark ->
                val colors = generatedColorScheme(seed, dark)
                assertReadable(colors.primary, colors.onPrimary)
                assertReadable(colors.primaryContainer, colors.onPrimaryContainer)
                assertReadable(colors.surface, colors.onSurface)
                assertReadable(colors.surfaceContainer, colors.onSurface)
                assertReadable(colors.secondaryContainer, colors.onSecondaryContainer)
                assertReadable(colors.tertiaryContainer, colors.onTertiaryContainer)
            }
        }
    }

    @Test fun customColorChangesTheSchemeAndGrayIsActuallyNeutral() {
        assertNotEquals(generatedColorScheme(RGB_RED, false).primary, generatedColorScheme(RGB_BLUE, false).primary)
        listOf(false, true).forEach { dark ->
            val gray = generatedColorScheme(RGB_CUSTOM, dark, monochrome = true)
            listOf(gray.primary, gray.surface, gray.surfaceContainer).forEach { color ->
                assertEquals(color.red, color.green, COMPONENT_TOLERANCE)
                assertEquals(color.green, color.blue, COMPONENT_TOLERANCE)
            }
        }
    }

    private fun assertReadable(
        background: Color,
        text: Color,
    ) {
        val light = maxOf(background.luminance(), text.luminance())
        val dark = minOf(background.luminance(), text.luminance())
        assertTrue((light + LUMINANCE_OFFSET) / (dark + LUMINANCE_OFFSET) >= MIN_TEXT_CONTRAST)
    }
}

private const val RGB_WHITE = 0xFFFFFF
private const val RGB_RED = 0xFF0000
private const val RGB_GREEN = 0x00FF00
private const val RGB_BLUE = 0x0000FF
private const val RGB_CUSTOM = 0x23A080
private const val COMPONENT_TOLERANCE = 0.005f
private const val LUMINANCE_OFFSET = 0.05f
private const val MIN_TEXT_CONTRAST = 4.5f
