package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.navigation.AppAnimatedNavHost
import com.solthariel.pawchan.navigation.InstantNavigator
import com.solthariel.pawchan.navigation.appComposable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Checks drawn pixels, not only whether screen nodes remain in the semantics tree. */
class AppNavigationPixelsRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var controller: NavHostController

    private fun setUpScreens() {
        compose.setContent {
            MaterialTheme {
                controller = rememberNavController(remember { InstantNavigator() })
                Box(Modifier.size(220.dp).testTag("pixel-host")) {
                    AppAnimatedNavHost(controller, "history") {
                        appComposable("history") {
                            Box(Modifier.fillMaxSize().background(Color.Red)) {
                                Button(
                                    onClick = { controller.navigate("thread") },
                                    modifier = Modifier.align(Alignment.BottomCenter).testTag("open"),
                                ) { Text("Open thread") }
                            }
                        }
                        appComposable("thread") {
                            Box(Modifier.fillMaxSize().background(Color.Green)) {
                                Button(
                                    onClick = { controller.popBackStack() },
                                    modifier = Modifier.align(Alignment.BottomCenter).testTag("back"),
                                ) { Text("Back to history") }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun firstFrameShowsOnlyDestinationColor() {
        setUpScreens()
        repeat(12) { iteration ->
            val forward = iteration % 2 == 0
            val before = if (forward) Color.Red else Color.Green
            val after = if (forward) Color.Green else Color.Red
            val route = if (forward) "thread" else "history"
            val initial = snapshot()
            assertTrue("Initial pixels do not match: $initial", matches(before, initial))
            compose.mainClock.autoAdvance = false
            val clockBefore = compose.mainClock.currentTime
            compose.onNodeWithTag(if (forward) "open" else "back").performClick()
            compose.runOnIdle { assertEquals(route, controller.currentDestination?.route) }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            val first = snapshot()
            if (!matches(after, first)) {
                val next =
                    (1..3).joinToString("\n") { frame ->
                        compose.mainClock.advanceTimeByFrame()
                        compose.waitForIdle()
                        "diagnostic extra frame $frame: ${snapshot()}"
                    }
                throw AssertionError(
                    "Iteration $iteration ($route), clock before click=$clockBefore ms. " +
                        "Expected $after on first checked frame; $first\n$next",
                )
            }
            // Drain completed navigation before the next independent measurement.
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
        }
    }

    private fun snapshot(): FrameSnapshot {
        val beforeCapture = compose.mainClock.currentTime
        val pixels = compose.onNodeWithTag("pixel-host").captureToImage().toPixelMap()
        val samples = listOf(1, 2, 3).map { pixels[pixels.width * it / 4, pixels.height / 4] }
        val oldNodes = compose.onAllNodesWithText("Open thread").fetchSemanticsNodes().size
        val newNodes = compose.onAllNodesWithText("Back to history").fetchSemanticsNodes().size
        return FrameSnapshot(beforeCapture, compose.mainClock.currentTime, samples, oldNodes, newNodes)
    }

    private fun matches(
        expected: Color,
        snapshot: FrameSnapshot,
    ): Boolean =
        snapshot.samples.all { color ->
            abs(color.red - expected.red) < 0.03f &&
                abs(color.green - expected.green) < 0.03f &&
                abs(color.blue - expected.blue) < 0.03f
        }

    private data class FrameSnapshot(
        val clockBeforeCapture: Long,
        val clockAfterCapture: Long,
        val samples: List<Color>,
        val historyNodes: Int,
        val threadNodes: Int,
    )
}
