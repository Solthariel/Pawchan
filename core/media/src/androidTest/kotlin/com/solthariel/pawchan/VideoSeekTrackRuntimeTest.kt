package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.video.VideoSeekTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class VideoSeekTrackRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun playedAndRemainingSectionsAreStraight() = verifyStraight(LayoutDirection.Ltr)

    @Test fun rightToLeftSectionsAreStraightAndReversed() = verifyStraight(LayoutDirection.Rtl)

    private fun verifyStraight(direction: LayoutDirection) {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f), LocalLayoutDirection provides direction) {
                MaterialTheme(colorScheme = darkColorScheme(primary = Color.White, onSurfaceVariant = Color.White)) {
                    Box(
                        Modifier
                            .width(240.dp)
                            .height(24.dp)
                            .background(Color.Black)
                            .testTag("track"),
                        contentAlignment = Alignment.Center,
                    ) { VideoSeekTrack(fraction = 0.5f, enabled = true) }
                }
            }
        }
        val pixels = compose.onNodeWithTag("track").captureToImage().toPixelMap()
        val middle = pixels.height / 2
        var offCenter = 0
        for (x in 12 until pixels.width - 12) {
            for (y in 0 until pixels.height) {
                if (abs(y - middle) >= 3 && pixels[x, y].red > 0.2f) offCenter++
            }
        }
        assertEquals("Both portions must be straight", 0, offCenter)
        val active = if (direction == LayoutDirection.Ltr) pixels.width / 4 else pixels.width * 3 / 4
        val inactive = if (direction == LayoutDirection.Ltr) pixels.width * 3 / 4 else pixels.width / 4
        assertTrue(pixels[active, middle].red > 0.9f)
        assertTrue(pixels[inactive, middle].red in 0.2f..0.7f)
    }
}
