package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.Color
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class ImageEditPreviewRuntimeTest {
    @Test
    fun previewContainsRenderedRedactionNotOriginalPixels() {
        val source = Bitmap.createBitmap(100, 80, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val bytes =
            ByteArrayOutputStream().use { stream ->
                source.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.toByteArray()
            }
        source.recycle()
        val mark =
            DraftRedaction(
                listOf(DraftPoint(.25f, .25f), DraftPoint(.75f, .75f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.RECTANGLE,
                .1f,
            )
        val preview = renderImageEditPreview(bytes, DraftImageEdit(marks = listOf(mark)))
        try {
            assertEquals(Color.BLACK, preview.getPixel(50, 40))
            assertEquals(Color.RED, preview.getPixel(5, 5))
        } finally {
            preview.recycle()
        }
    }

    @Test
    fun rejectsVideoAndGifInsteadOfShowingUneditedContent() {
        assertThrows(IllegalArgumentException::class.java) {
            renderImageEditPreview("GIF89a".encodeToByteArray(), DraftImageEdit())
        }
    }
}
