package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.formatByteSizeForUi
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.io.ByteArrayOutputStream

class AttachmentReencodingSummaryRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pendingFormatAndDimensionsUpdateWithoutMutatingAttachment() {
        val bytes =
            ByteArrayOutputStream().use { output ->
                val bitmap = Bitmap.createBitmap(100, 200, Bitmap.Config.ARGB_8888)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                bitmap.recycle()
                output.toByteArray()
            }
        val file = ComposerAttachment(ReplyAttachment("photo.png", "image/png", bytes))
        val settings = mutableStateOf(ImageReencoding())
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) { AttachmentReencodingSummary(file, settings.value) }
            }
        }
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("· 100×200 ·", substring = true),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        compose.onNodeWithText("photo.jpg", useUnmergedTree = true).assertExists()
        compose.runOnIdle { settings.value = ImageReencoding(jpeg = false, scaleDivisor = 4) }
        compose.onNodeWithText("· 25×50 ·", substring = true, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("photo.png", useUnmergedTree = true).assertExists()
        val pending = file.copy(options = file.options.copy(reencoding = settings.value))
        val size = formatByteSizeForUi(requireNotNull(encodedAttachmentSize(pending)))
        compose.waitUntil(5_000) {
            compose
                .onAllNodes(
                    androidx.compose.ui.test
                        .hasText("· 25×50 · $size"),
                    useUnmergedTree = true,
                ).fetchSemanticsNodes()
                .isNotEmpty()
        }
        val name = compose.onNodeWithText("photo.png", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val details =
            compose.onNodeWithText("· 25×50 · $size", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(160f * compose.density.density, (name.left + details.right) / 2f, 1f)
        assertEquals(name.top, details.top, 1f)
        assertEquals(4f * compose.density.density, details.left - name.right, 1f)
        assertNull(file.options.reencoding)
    }
}
