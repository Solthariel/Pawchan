package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.core.graphics.scale
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelRedactionTransparencyRuntimeTest {
    @Test
    fun rectangleReplacesTransparentAndOpaquePixelsWithoutChangingOutside() {
        checkRedaction(DraftRedactionTool.RECTANGLE)
    }

    @Test
    fun brushReplacesTransparentAndOpaquePixelsWithoutChangingOutside() {
        checkRedaction(DraftRedactionTool.BRUSH)
    }

    private fun checkRedaction(tool: DraftRedactionTool) {
        val source = sampleBitmap()
        val baseline = rasterizeImageEdit(source, DraftImageEdit())
        val scaled = baseline.scale(32, 32, filter = false)
        val expected = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
        val points =
            if (tool == DraftRedactionTool.RECTANGLE) {
                listOf(DraftPoint(.2f, .2f), DraftPoint(.8f, .8f))
            } else {
                listOf(DraftPoint(.15f, .5f), DraftPoint(.85f, .5f))
            }
        val redaction = DraftRedaction(points, DraftRedactionMode.PIXELS, tool, .2f)
        val actual = rasterizeImageEdit(source, DraftImageEdit(marks = listOf(redaction)))
        try {
            Canvas(expected).drawBitmap(scaled, null, RectF(0f, 0f, 96f, 96f), Paint())
            val ys = if (tool == DraftRedactionTool.RECTANGLE) 25 until 70 else 45 until 51
            var changed = false
            var cleared = false
            var partial = false
            for (y in ys) {
                for (x in 25 until 70) {
                    val want = expected.getPixel(x, y)
                    val original = baseline.getPixel(x, y)
                    assertEquals("Incorrect pixel at ($x, $y)", want, actual.getPixel(x, y))
                    if (want != original) changed = true
                    if (Color.alpha(want) == 0 && Color.alpha(original) == 255) cleared = true
                    if (Color.alpha(want) in 1..254) partial = true
                }
            }
            assertTrue("Mosaic did not change any pixels", changed)
            assertTrue("Fixture did not exercise replacing opaque pixels with transparent ones", cleared)
            assertTrue("Fixture did not exercise partially transparent pixels", partial)
            for (y in 0 until 96) {
                for (x in 0 until 96) {
                    val outside =
                        if (tool == DraftRedactionTool.RECTANGLE) {
                            x < 18 || x >= 78 || y < 18 || y >= 78
                        } else {
                            y < 37 || y >= 59
                        }
                    if (outside) {
                        assertEquals(
                            "Pixel outside the mask changed at ($x, $y)",
                            baseline.getPixel(x, y),
                            actual.getPixel(x, y),
                        )
                    }
                }
            }
        } finally {
            actual.recycle()
            expected.recycle()
            if (scaled !== baseline) scaled.recycle()
            baseline.recycle()
            source.recycle()
        }
    }

    private fun sampleBitmap(): Bitmap =
        Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    // Opaque stripes are narrower than the mosaic blocks and embedded in a wide
                    // transparent area. Some stripes are omitted by nearest-neighbour downsampling,
                    // so upsampling has transparent pixels over previously opaque pixels.
                    val alpha =
                        when {
                            x in 27..61 && (x - 30) % 4 == 0 -> 255
                            x in 27..61 -> 0
                            else -> 128
                        }
                    setPixel(x, y, Color.argb(alpha, x, y, x + y))
                }
            }
        }
}
