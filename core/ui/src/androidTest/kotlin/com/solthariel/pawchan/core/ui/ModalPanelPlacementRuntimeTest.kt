package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ModalPanelPlacementRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun resizingThePanelDoesNotWaitAnotherCompositionToRemoveItsScrim() {
        val height = mutableStateOf(80.dp)
        compose.setContent {
            MaterialTheme {
                // Avoid Android 15's navigation-bar contrast overlay in the scrim sample.
                Box(Modifier.fillMaxSize().systemBarsPadding()) {
                    ModalLayerHost {
                        ModalInlinePanel {
                            Box(Modifier.fillMaxSize().background(Color.White)) {
                                Box(
                                    Modifier
                                        .padding(top = 64.dp)
                                        .fillMaxWidth()
                                        .height(height.value)
                                        .modalPanelBounds(0.dp)
                                        .background(Color.Blue)
                                        .testTag("panel"),
                                )
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { height.value = 180.dp }
        compose.mainClock.advanceTimeByFrame()
        val pixels = compose.onNodeWithTag("modal-layer-host", useUnmergedTree = true).captureToImage().toPixelMap()
        val panel = compose.onNodeWithTag("panel").fetchSemanticsNode().boundsInRoot
        val host = compose.onNodeWithTag("modal-layer-host", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val x = (panel.center.x - host.left).toInt()
        val y = (panel.bottom - host.top - 4).toInt()
        assertEquals(1f, pixels[x, y].blue, .02f)
        assertEquals(.4f, pixels[pixels.width - 2, pixels.height - 2].red, .02f)
    }
}
