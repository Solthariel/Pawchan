package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ImageLiveRedactionOverlayRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun fillBrushDrawsAContinuousOpaqueLineWithoutHidingTheRest() {
        val brush =
            DraftRedaction(
                listOf(DraftPoint(.1f, .5f), DraftPoint(.9f, .5f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.BRUSH,
                .1f,
            )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(100.dp).background(Color.White).testTag("preview")) {
                    ImagePendingMarkOverlay(
                        DraftImageEdit(marks = listOf(brush)),
                        brush,
                        ReencodingDimensions(100, 100),
                        Modifier.matchParentSize(),
                    )
                }
            }
        }
        val pixels = compose.onNodeWithTag("preview").captureToImage().toPixelMap()
        val center = pixels[pixels.width / 2, pixels.height / 2]
        val outside = pixels[pixels.width / 2, pixels.height / 4]
        assertTrue(center.red < .1f && center.green < .1f && center.blue < .1f)
        assertTrue(outside.red > .9f && outside.green > .9f && outside.blue > .9f)
    }

    @Test fun transparentPixelationCannotRevealUnderlyingImageInPreview() {
        val brush =
            DraftRedaction(
                listOf(DraftPoint(.1f, .5f), DraftPoint(.9f, .5f)),
                DraftRedactionMode.PIXELS,
                DraftRedactionTool.BRUSH,
                .1f,
            )
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(100.dp).background(Color.Red).testTag("preview")) {
                    ImagePendingMarkOverlay(
                        DraftImageEdit(marks = listOf(brush)),
                        brush,
                        ReencodingDimensions(100, 100),
                        Modifier.matchParentSize(),
                    )
                }
            }
        }
        compose.waitForIdle()
        val pixels = compose.onNodeWithTag("preview").captureToImage().toPixelMap()
        val center = pixels[pixels.width / 2, pixels.height / 2]
        assertTrue(center.red < .1f && center.green < .1f && center.blue < .1f)
    }
}
