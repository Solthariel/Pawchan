package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
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
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class VideoSeekTrackStaticRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun enabledTrackDoesNotAnimateAtFixedProgress() = verifyStatic(enabled = true)

    @Test fun disabledTrackDoesNotAnimateAtFixedProgress() = verifyStatic(enabled = false)

    private fun verifyStatic(enabled: Boolean) {
        compose.setContent {
            MaterialTheme {
                Box(
                    Modifier
                        .width(240.dp)
                        .height(24.dp)
                        .background(Color.Black)
                        .testTag("track"),
                    contentAlignment = Alignment.Center,
                ) { VideoSeekTrack(fraction = 0.5f, enabled = enabled) }
            }
        }
        val first = compose.onNodeWithTag("track").captureToImage().toPixelMap()
        compose.mainClock.advanceTimeBy(600)
        val second = compose.onNodeWithTag("track").captureToImage().toPixelMap()
        assertEquals(first.width, second.width)
        assertEquals(first.height, second.height)
        for (x in 0 until first.width) {
            for (y in 0 until first.height) {
                assertEquals("Track changed at ($x, $y)", first[x, y], second[x, y])
            }
        }
    }
}
