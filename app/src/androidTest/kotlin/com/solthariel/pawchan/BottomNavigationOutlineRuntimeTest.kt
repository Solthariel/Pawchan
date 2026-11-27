package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import com.solthariel.pawchan.navigation.chrome.APP_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomBar
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class BottomNavigationOutlineRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private var density = 1f
    private var clicks = 0

    @Test fun plainDarkPanelHasFlatTranslucentSurface() = checkSurface(false)

    @Test fun plainLightPanelHasFlatTranslucentSurface() = checkSurface(true)

    @Test
    fun allButtonsStillDispatchTheirActions() {
        show()
        listOf("Каталог", "Треды", "Доски", "Настройки", "Написать").forEach {
            compose.onNodeWithContentDescription(it).performClick()
        }
        compose.runOnIdle { assertEquals(5, clicks) }
    }

    private fun checkSurface(light: Boolean) {
        show(light)
        val colors = if (light) lightColorScheme() else darkColorScheme()
        val bar = compose.onNodeWithTag("navigation-bar").fetchSemanticsNode().boundsInRoot
        assertEquals(APP_BOTTOM_BAR_HEIGHT.value * density, bar.height, 1f)
        val pixels = compose.onNodeWithTag("navigation-outline").captureToImage().toPixelMap()
        val middleX = pixels.width / 2
        val sideX = (10 * density).roundToInt()
        val middleY = (18 * density).roundToInt()
        val bottomY = (55 * density).roundToInt().coerceAtMost(pixels.height - 1)
        val expected = colors.surface.copy(alpha = 0.8f).compositeOver(Color.Magenta)
        assertSurfaceColor(expected, pixels[sideX, middleY])
        assertSurfaceColor(expected, pixels[middleX, bottomY])
        // The 8dp fade starts transparent, not with an outline/divider.
        val firstPixelAlpha = 0.8f * (0.5f / density / 8f)
        assertSurfaceColor(
            colors.surface.copy(alpha = firstPixelAlpha).compositeOver(Color.Magenta),
            pixels[middleX, 0],
        )
    }

    private fun show(light: Boolean = false) {
        compose.setContent {
            density = LocalDensity.current.density
            MaterialTheme(colorScheme = if (light) lightColorScheme() else darkColorScheme()) {
                CompositionLocalProvider(
                    LocalPostDisplayPreferences provides PostDisplayPreferences(),
                ) {
                    Box(
                        Modifier
                            .width(360.dp)
                            .height(80.dp)
                            // This fixture checks the panel drawing, not system-bar padding.
                            // Consume the real device inset so it cannot shrink the 56dp surface.
                            .consumeWindowInsets(WindowInsets.navigationBars)
                            .background(Color.Magenta)
                            .testTag("navigation-outline"),
                    ) {
                        AppBottomBar(
                            AppBottomActions({ clicks++ }, { clicks++ }, { clicks++ }, { clicks++ }, { clicks++ }),
                            AppBottomDestination.CATALOG,
                            modifier = Modifier.testTag("navigation-bar"),
                        )
                    }
                }
            }
        }
    }

    // Native screenshot color conversion may round a channel by one 8-bit level.
    private fun assertSurfaceColor(
        expected: Color,
        actual: Color,
    ) {
        val tolerance = 1.01f / 255f
        assertTrue(kotlin.math.abs(expected.red - actual.red) <= tolerance)
        assertTrue(kotlin.math.abs(expected.green - actual.green) <= tolerance)
        assertTrue(kotlin.math.abs(expected.blue - actual.blue) <= tolerance)
        assertEquals(expected.alpha, actual.alpha)
    }
}
