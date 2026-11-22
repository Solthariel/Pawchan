package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.FloatingTopChrome
import com.solthariel.pawchan.core.ui.LocalFloatingTopEnabled
import com.solthariel.pawchan.core.ui.LocalFloatingTopStatusInset
import com.solthariel.pawchan.core.ui.LocalTopBarTransparency
import com.solthariel.pawchan.core.ui.LocalTopOverlayInset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FloatingTopChromeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun listScrollsBehindToolbarAndStatusArea() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = Color.Black)) {
                CompositionLocalProvider(
                    LocalFloatingTopEnabled provides true,
                    LocalFloatingTopStatusInset provides 20.dp,
                    LocalTopBarTransparency provides 100,
                ) {
                    Box(Modifier.fillMaxSize().background(Color.White).testTag("floating-root")) {
                        FloatingTopChrome(toolbar = { Box(Modifier.fillMaxWidth().height(64.dp)) }) {
                            LazyColumn(
                                Modifier.fillMaxSize().testTag("floating-list"),
                                contentPadding = PaddingValues(top = LocalTopOverlayInset.current),
                            ) {
                                items((1..40).toList()) {
                                    Box(Modifier.fillMaxWidth().height(80.dp).background(Color.Green))
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.onNodeWithTag("floating-list").performScrollToIndex(5)
        val pixels = compose.onNodeWithTag("floating-root").captureToImage().toPixelMap()
        val density = compose.density.density
        listOf(10, 55).forEach { y ->
            val sample = pixels[pixels.width / 2, (y * density).toInt()]
            assertTrue(sample.green > 0.9f && sample.red < 0.1f)
        }
    }

    @Test fun transparencyAffectsStatusAndToolbarBackgroundButNotText() {
        val percent = mutableIntStateOf(0)
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(background = Color.Black)) {
                CompositionLocalProvider(
                    LocalFloatingTopEnabled provides true,
                    LocalFloatingTopStatusInset provides 20.dp,
                    LocalTopBarTransparency provides percent.intValue,
                ) {
                    Box(Modifier.fillMaxSize().background(Color.White).testTag("chrome-test")) {
                        FloatingTopChrome(toolbar = {
                            Box(Modifier.fillMaxWidth().height(64.dp)) { Text("TOP", color = Color.Red) }
                        }) {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items((1..20).toList()) { Box(Modifier.fillMaxWidth().height(50.dp)) }
                            }
                        }
                    }
                }
            }
        }
        listOf(0, 50, 100).forEach { value ->
            compose.runOnIdle { percent.intValue = value }
            val pixels = compose.onNodeWithTag("chrome-test").captureToImage().toPixelMap()
            // A point in the status area and a point in the toolbar (away from the text).
            val density = compose.density.density
            listOf(10, 55).forEach { dp ->
                val sample = pixels[pixels.width - 5, (dp * density).toInt()]
                assertEquals(value / 100f, sample.red, 0.02f)
                assertEquals(value / 100f, sample.green, 0.02f)
                assertEquals(value / 100f, sample.blue, 0.02f)
            }
            var redPixels = 0
            for (y in (20 * density).toInt() until (70 * density).toInt().coerceAtMost(pixels.height)) {
                for (x in 0 until pixels.width) {
                    val pixel = pixels[x, y]
                    if (pixel.red > 0.95f && pixel.green < 0.2f && pixel.blue < 0.2f) redPixels++
                }
            }
            assertTrue(redPixels > 0)
        }
    }
}
