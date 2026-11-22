package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.video.VideoSeekTrack
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VideoSeekPaletteRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun suppliedAppPaletteIsUsedInsteadOfMediaOverlayPalette() {
        val appColors = darkColorScheme(primary = Color.Red, onSurfaceVariant = Color.Green)
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color.Blue, onSurfaceVariant = Color.Blue)) {
                Box(
                    Modifier
                        .width(240.dp)
                        .height(24.dp)
                        .background(Color.Black)
                        .testTag("track"),
                    contentAlignment = Alignment.Center,
                ) {
                    VideoSeekTrack(fraction = 0.5f, enabled = true, colors = appColors)
                }
            }
        }
        val pixels = compose.onNodeWithTag("track").captureToImage().toPixelMap()
        val played = pixels[pixels.width / 4, pixels.height / 2]
        val remaining = pixels[pixels.width * 3 / 4, pixels.height / 2]
        assertTrue(played.red > 0.9f && played.blue < 0.1f)
        assertTrue(remaining.green in 0.5f..0.6f && remaining.blue < 0.1f)
    }
}
