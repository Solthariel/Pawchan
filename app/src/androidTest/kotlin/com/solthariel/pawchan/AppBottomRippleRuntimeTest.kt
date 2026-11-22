package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomBar
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class AppBottomRippleRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun allFivePressIndicationsStayCircularWhileWholeCellIsClickable() {
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    AppBottomBar(AppBottomActions({}, {}, {}, {}, {}), AppBottomDestination.CATALOG)
                }
            }
        }
        compose.mainClock.autoAdvance = false
        listOf("Каталог", "Треды", "Доски", "Настройки", "Написать").forEach { label ->
            val target = compose.onNodeWithContentDescription(label)
            val before = target.captureToImage().toPixelMap()
            target.performTouchInput { down(center) }
            compose.mainClock.advanceTimeBy(500)
            val inset = with(compose.density) { 2.dp.roundToPx() }.coerceAtLeast(1)
            // The Material ripple may run on RenderThread, not Compose's virtual clock.
            // Probe inside the circle but outside the 24dp icon; keep both exterior checks.
            val x = before.width / 2
            val y = before.height / 2 - with(compose.density) { 16.dp.roundToPx() }
            var pressed: PixelMap = before
            compose.waitUntil(3_000) {
                pressed = target.captureToImage().toPixelMap()
                pressed[x, y] != before[x, y]
            }
            assertNotEquals(before[x, y], pressed[x, y])
            assertEquals(before[inset, before.height / 2], pressed[inset, pressed.height / 2])
            val right = before.width - 1 - inset
            assertEquals(before[right, before.height / 2], pressed[right, pressed.height / 2])
            target.performTouchInput { cancel() }
            compose.mainClock.advanceTimeBy(500)
        }
        compose.mainClock.autoAdvance = true
    }

    @Test fun touchingOutsideTheVisualCircleStillInvokesTheAction() {
        var writes = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    AppBottomBar(AppBottomActions({}, {}, {}, {}, { writes++ }), AppBottomDestination.CATALOG)
                }
            }
        }
        compose.onNodeWithContentDescription("Написать").performTouchInput {
            click(Offset(1f, center.y))
        }
        compose.runOnIdle { assertEquals(1, writes) }
    }
}
