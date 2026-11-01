package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomBar
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.LocalBottomBarTransparency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppBottomTransparencyRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun changedTransparencyUpdatesTheSurfaceWithoutFadingIcons() {
        val percent = mutableIntStateOf(20)
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(surface = Color.Black, onSurfaceVariant = Color.Red)) {
                CompositionLocalProvider(LocalBottomBarTransparency provides percent.intValue) {
                    Box(Modifier.width(320.dp).background(Color.White)) {
                        AppBottomBar(AppBottomActions({}, {}, {}, {}, {}), AppBottomDestination.CATALOG)
                    }
                }
            }
        }
        listOf(20, 0, 100).forEach { value ->
            compose.runOnIdle { percent.intValue = value }
            val pixels = compose.onNodeWithContentDescription("Написать").captureToImage().toPixelMap()
            val sample = pixels[1, pixels.height / 2]
            assertEquals(value / 100f, sample.red, 0.02f)
            assertEquals(value / 100f, sample.green, 0.02f)
            assertEquals(value / 100f, sample.blue, 0.02f)
            var redPixels = 0
            for (y in 0 until pixels.height) {
                for (x in 0 until pixels.width) {
                    val pixel = pixels[x, y]
                    if (pixel.red > 0.99f && pixel.green < 0.02f && pixel.blue < 0.02f) redPixels++
                }
            }
            assertTrue(redPixels > 0)
        }
    }
}
