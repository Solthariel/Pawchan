package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PullRefreshMorphRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun topPullShowsFilledMorphInsteadOfArc() = assertFilledMorph(PullRefreshSide.TOP)

    @Test fun bottomPullShowsFilledMorphInsteadOfArc() = assertFilledMorph(PullRefreshSide.BOTTOM)

    private fun assertFilledMorph(side: PullRefreshSide) {
        val gesture = PullRefreshGesture()
        gesture.updateHeight(400, tablet = false, density = 1f)
        compose.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                MaterialTheme(colorScheme = darkColorScheme(primary = Color.Magenta, onPrimary = Color.Black)) {
                    val visual = rememberPullRefreshVisual(gesture.release)
                    Box(Modifier.size(400.dp).background(Color.White)) {
                        PullRefreshIndicator(gesture, visual, 30.dp, Modifier.fillMaxSize().testTag("indicator"))
                    }
                }
            }
        }
        compose.runOnIdle { gesture.consumeEdge(if (side == PullRefreshSide.TOP) 48f else -48f, true) }
        val pixels = compose.onNodeWithTag("indicator").captureToImage().toPixelMap()
        val y = if (side == PullRefreshSide.TOP) 12 else pixels.height - 30 - 12
        val center = pixels[pixels.width / 2, y]
        assertTrue("The center must be filled, not an empty arc", center.red < 0.1f && center.blue < 0.1f)
    }
}
