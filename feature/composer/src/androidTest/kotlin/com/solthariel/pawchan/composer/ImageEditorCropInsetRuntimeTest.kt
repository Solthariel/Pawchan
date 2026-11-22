package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class ImageEditorCropInsetRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cropFrameStaysAwayFromViewportEdgesForTallImage() {
        val image = Bitmap.createBitmap(100, 800, Bitmap.Config.ARGB_8888)
        val bytes =
            ByteArrayOutputStream().use {
                image.compress(Bitmap.CompressFormat.PNG, 100, it)
                it.toByteArray()
            }
        image.recycle()
        val state = ImageEditorState(DraftImageEdit())
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(300.dp, 500.dp).testTag("viewport")) {
                    ImageEditorCanvas(bytes, state, ImageEditorCanvasConfig(ImageEditorCanvasMode.CROP), {})
                }
            }
        }
        compose.waitUntil(timeoutMillis = 10_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasContentDescription("Рамка обрезки"),
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        val viewport = compose.onNodeWithTag("viewport").fetchSemanticsNode().boundsInRoot
        val crop = compose.onNodeWithContentDescription("Рамка обрезки").fetchSemanticsNode().boundsInRoot
        val imageFrame = compose.onNodeWithTag("editor-image-frame").fetchSemanticsNode().boundsInRoot
        // The interactive overlay deliberately reaches into the padding, while the image stays inset.
        assertTrue(imageFrame.top > viewport.top)
        assertTrue(imageFrame.bottom < viewport.bottom)
        assertTrue(imageFrame.left > viewport.left)
        assertTrue(imageFrame.right < viewport.right)
        assertTrue(crop.left < imageFrame.left && crop.top < imageFrame.top)
        assertTrue(crop.right > imageFrame.right && crop.bottom > imageFrame.bottom)
    }
}
