package com.solthariel.pawchan.media

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MediaOpeningRuntimeTest {
    @get:Rule val compose =
        createComposeRule(
            effectContext = TestMotionDurationScale,
        )

    @Test
    fun viewerBecomesInteractiveAfterOpeningAnimation() {
        compose.mainClock.autoAdvance = false
        val item = image("one")
        val store = MediaThumbnailOriginStore().apply { put(snapshot(item)) }
        var clicks = 0
        compose.setContent {
            CompositionLocalProvider(LocalMediaThumbnailOrigin provides store) {
                MediaImageOpening(item) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { clicks++ }, modifier = Modifier.testTag("viewer")) { Text("Viewer") }
                    }
                }
            }
        }
        // Let layout report non-zero viewer bounds and start the controlled animation.
        compose.mainClock.advanceTimeByFrame()
        compose.onRoot().performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(0, clicks) }
        // Advance the virtual clock by the transition duration, not a wall-clock sleep.
        compose.mainClock.advanceTimeBy(250)
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithTag("viewer").assertIsDisplayed()
        compose.onRoot().performTouchInput { click(center) }
        compose.runOnIdle { assertEquals(1, clicks) }
    }

    @Test
    fun missingOrWrongOriginOpensViewerNormally() {
        val item = image("one")
        val store = MediaThumbnailOriginStore().apply { put(snapshot(image("different"))) }
        compose.setContent {
            CompositionLocalProvider(LocalMediaThumbnailOrigin provides store) {
                MediaImageOpening(item) { Text("Viewer", Modifier.testTag("viewer")) }
            }
        }
        compose.onNodeWithTag("viewer").assertIsDisplayed()
    }

    private fun image(id: String): MediaAttachmentUi =
        MediaAttachmentUi(
            source = MediaAttachmentSource(id, "$id.jpg", "https://example.org/$id.jpg", null),
            byteSize = null,
            viewer = AttachmentViewer.IMAGE,
        )

    private fun snapshot(item: MediaAttachmentUi): MediaThumbnailSnapshot =
        MediaThumbnailSnapshot(
            key = item.thumbnailKey(),
            bounds = Rect(0f, 0f, 1_000f, 1_000f),
            cornerPx = 8f,
            bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888),
        )

    private object TestMotionDurationScale : MotionDurationScale {
        override val scaleFactor = 1f
    }
}
