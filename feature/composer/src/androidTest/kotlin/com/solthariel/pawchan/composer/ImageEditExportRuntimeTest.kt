package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream

class ImageEditExportRuntimeTest {
    @Test
    fun pngExportBakesBlackRedactionAndPreservesTheSourceBytes() {
        val source = image(Bitmap.CompressFormat.PNG)
        val original = source.copyOf()
        val mark =
            DraftRedaction(
                listOf(DraftPoint(.25f, .25f), DraftPoint(.75f, .75f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.RECTANGLE,
                .1f,
            )
        val result = exportImageEdit(source, DraftImageEdit(marks = listOf(mark)), null)
        assertEquals("png", result.extension)
        assertEquals("image/png", result.mime)
        assertArrayEquals(original, source)
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(result.content, 0, result.content.size))
        try {
            assertEquals(Color.BLACK, decoded.getPixel(6, 10))
            assertEquals(Color.RED, decoded.getPixel(0, 0))
        } finally {
            decoded.recycle()
        }
    }

    @Test
    fun cropRotationAndScaleAreAppliedBeforeJpegEncoding() {
        val source = image(Bitmap.CompressFormat.PNG)
        val edit = DraftImageEdit(crop = DraftCrop(.25f, .25f, .75f, .75f), quarterTurns = 1)
        val result = exportImageEdit(source, edit, DraftImageReencoding(jpeg = true, scaleDivisor = 2))
        assertEquals("jpg", result.extension)
        assertEquals("image/jpeg", result.mime)
        val decoded = requireNotNull(BitmapFactory.decodeByteArray(result.content, 0, result.content.size))
        try {
            assertEquals(5, decoded.width)
            assertEquals(3, decoded.height)
        } finally {
            decoded.recycle()
        }
    }

    @Test
    fun staticWebpDefaultsToPngAndGifIsRejected() {
        val webp = image(Bitmap.CompressFormat.WEBP_LOSSLESS)
        val result = exportImageEdit(webp, DraftImageEdit(), null)
        assertEquals("png", result.extension)
        assertEquals("image/png", result.mime)
        assertThrows(IllegalArgumentException::class.java) {
            exportImageEdit("GIF89a".encodeToByteArray(), DraftImageEdit(), null)
        }
    }

    private fun image(format: Bitmap.CompressFormat): ByteArray {
        val bitmap = Bitmap.createBitmap(12, 20, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        return try {
            ByteArrayOutputStream().use { output ->
                check(bitmap.compress(format, 100, output))
                output.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
