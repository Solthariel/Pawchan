package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.VideoThumbnailBadge
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class VideoThumbnailBadgeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun neutralScrimAndOpaqueFilledGlyphOnLightAndDarkFrames() {
        val dark = Color(0xFF202020)
        compose.setContent {
            Row {
                TestTile("light", 88, Color.White) { VideoThumbnailBadge() }
                TestTile("dark", 88, dark) { VideoThumbnailBadge() }
                TestTile("photo", 88, Color.White) {}
            }
        }
        for ((tag, channel) in listOf("light" to 255, "dark" to 32)) {
            val pixels = tile(tag)
            assertGray((channel * 0.4f).roundToInt(), gray(pixels, 0f, -11f), 2)
            assertGray(255, gray(pixels, 0f, 0f), 2)
            assertGray(channel, gray(pixels, -15f, -15f), 2)
        }
        assertGray(255, gray(tile("photo"), 0f, -11f), 0)
    }

    @Test fun compactBadgeFitsSmallSavedFileThumbnailWithoutDimmingItsCorners() {
        compose.setContent { TestTile("compact", 56, Color.White) { VideoThumbnailBadge(compact = true) } }
        val pixels = tile("compact")
        assertGray(102, gray(pixels, 0f, -9f), 2)
        assertGray(255, gray(pixels, 0f, -15f), 1)
        assertGray(255, gray(pixels, 0f, 0f), 1)
    }

    @Test fun badgeDoesNotStealTapLongPressOrCreateAnotherAccessibilityButton() {
        var taps = 0
        var longPresses = 0
        compose.setContent {
            Box(
                Modifier.size(88.dp).testTag("tile").combinedClickable(onClick = { taps++ }, onLongClick = { longPresses++ }),
                contentAlignment = Alignment.Center,
            ) { VideoThumbnailBadge() }
        }
        assertEquals(1, compose.onAllNodes(hasClickAction(), useUnmergedTree = true).fetchSemanticsNodes().size)
        compose.onNodeWithTag("tile").performTouchInput { click(center) }
        compose.onNodeWithTag("tile").performTouchInput { longClick(center) }
        compose.runOnIdle {
            assertEquals(1, taps)
            assertEquals(1, longPresses)
        }
    }

    private fun tile(tag: String): PixelMap = compose.onNodeWithTag(tag).captureToImage().toPixelMap()

    private fun gray(
        pixels: PixelMap,
        dx: Float,
        dy: Float,
    ): Int {
        val x = pixels.width / 2 + with(compose.density) { dx.dp.roundToPx() }
        val y = pixels.height / 2 + with(compose.density) { dy.dp.roundToPx() }
        val color = pixels[x, y]
        assertTrue(kotlin.math.abs(color.red - color.green) < 0.01f && kotlin.math.abs(color.red - color.blue) < 0.01f)
        return (color.red * 255).roundToInt()
    }

    private fun assertGray(
        expected: Int,
        actual: Int,
        tolerance: Int,
    ) = org.junit.Assert.assertEquals(expected.toDouble(), actual.toDouble(), tolerance.toDouble())
}

@Composable
private fun TestTile(
    tag: String,
    size: Int,
    color: Color,
    content: @Composable () -> Unit,
) {
    Box(Modifier.size(size.dp).background(color).testTag(tag), contentAlignment = Alignment.Center) { content() }
}
