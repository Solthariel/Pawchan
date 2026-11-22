package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.OWN_POST_MARKER_COLOR
import com.solthariel.pawchan.core.ui.REPLY_TO_ME_MARKER_COLOR
import com.solthariel.pawchan.thread.ThreadPostChanges
import com.solthariel.pawchan.thread.postCardMarker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostCardMarkerRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ownPostUsesFixedEmeraldColorAndFullHeight() {
        val pixels = marker(ThreadPostChanges(own = true))
        for (y in 12 until 116) assertEquals(OWN_POST_MARKER_COLOR, pixels[1, y])
        assertEquals(0f, pixels[1, 9].red, 0.01f)
        assertEquals(0f, pixels[1, 118].red, 0.01f)
    }

    @Test fun replyHasEqualSegmentsAndSameOuterBoundsAsSolid() {
        val pixels = marker(ThreadPostChanges(replyToMe = true))
        // 128dp card minus two 10dp insets: four 24dp segments, three 4dp gaps.
        for (start in listOf(10, 38, 66, 94)) {
            assertTrue(pixels[1, start].red > 0.5f)
            assertTrue(pixels[1, start + 23].red > 0.5f)
            assertEquals(REPLY_TO_ME_MARKER_COLOR, pixels[1, start + 12])
        }
        for (start in listOf(34, 62, 90)) {
            for (y in start until start + 4) assertEquals(0f, pixels[1, y].red, 0.01f)
        }
        assertEquals(0f, pixels[1, 9].red, 0.01f)
        assertEquals(0f, pixels[1, 118].red, 0.01f)
    }

    @Test fun ownPostTakesPriorityWhenItAlsoRepliesToMe() {
        val pixels = marker(ThreadPostChanges(own = true, replyToMe = true))
        for (y in 12 until 116) assertEquals(OWN_POST_MARKER_COLOR, pixels[1, y])
    }

    @Test fun unrelatedPostHasNoMarker() {
        val pixels = marker(ThreadPostChanges())
        for (y in 0 until pixels.height) assertEquals(0f, pixels[1, y].blue, 0.01f)
    }

    private fun marker(changes: ThreadPostChanges): PixelMap {
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(
                    lightColorScheme(onSurface = Color.White, primary = Color.Blue, tertiary = Color.Green),
                ) {
                    Box(
                        Modifier
                            .size(20.dp, 128.dp)
                            .background(Color.Black)
                            .postCardMarker(
                                changes,
                                ownColor = OWN_POST_MARKER_COLOR,
                                replyColor = REPLY_TO_ME_MARKER_COLOR,
                            ).testTag("marker"),
                    )
                }
            }
        }
        return compose.onNodeWithTag("marker").captureToImage().toPixelMap()
    }
}
