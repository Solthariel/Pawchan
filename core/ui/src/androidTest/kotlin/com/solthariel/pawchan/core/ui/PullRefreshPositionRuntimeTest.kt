package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PullRefreshPositionRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun topIndicatorDoesNotJumpWhenLoadingStarts() = assertSamePosition(PullRefreshSide.TOP)

    @Test fun bottomIndicatorDoesNotJumpWhenLoadingStarts() = assertSamePosition(PullRefreshSide.BOTTOM)

    private fun assertSamePosition(side: PullRefreshSide) {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 1f)
        compose.setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Color.Magenta, onPrimary = Color.Black)) {
                val visual = rememberPullRefreshVisual(gesture.release)
                Box(Modifier.size(400.dp).background(Color.Black)) {
                    PullRefreshIndicator(gesture, visual, 30.dp, Modifier.fillMaxSize().testTag("indicator"))
                }
            }
        }
        compose.runOnIdle { gesture.consumeEdge(if (side == PullRefreshSide.TOP) 192f else -192f, true) }
        val whilePulling = discCenterY()
        compose.runOnIdle { assertTrue(gesture.release(busy = false)) }
        compose.mainClock.advanceTimeBy(200)
        val whileLoading = discCenterY()
        assertEquals(whilePulling, whileLoading, 2f)
    }

    private fun discCenterY(): Float {
        val pixels = compose.onNodeWithTag("indicator").captureToImage().toPixelMap()
        var totalY = 0L
        var count = 0
        for (y in 0 until pixels.height) {
            for (x in 0 until pixels.width) {
                val color = pixels[x, y]
                if (color.red > 0.8f && color.green < 0.2f && color.blue > 0.8f) {
                    totalY += y
                    count++
                }
            }
        }
        assertTrue("Indicator was not visible", count > 0)
        return totalY.toFloat() / count
    }
}
