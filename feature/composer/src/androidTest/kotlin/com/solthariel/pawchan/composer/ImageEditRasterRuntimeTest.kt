package com.solthariel.pawchan.composer

import android.graphics.Bitmap
import android.graphics.Color
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.DraftStroke
import com.solthariel.pawchan.domain.drafts.DraftTextAppearance
import com.solthariel.pawchan.domain.drafts.DraftTextMark
import com.solthariel.pawchan.domain.drafts.DraftTextStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageEditRasterRuntimeTest {
    @Test
    fun everyQuarterTurnAndMirrorKeepsTheExpectedSourcePixels() {
        val source = sampleBitmap(4, 3)
        try {
            for (turns in 0..3) {
                for (mirrored in listOf(false, true)) {
                    val edited = rasterizeImageEdit(source, DraftImageEdit(quarterTurns = turns, mirrored = mirrored))
                    try {
                        for (y in 0 until source.height) {
                            for (x in 0 until source.width) {
                                val mx = if (mirrored) source.width - 1 - x else x
                                val target = targetPixel(mx, y, source.width, source.height, turns)
                                assertEquals(source.getPixel(x, y), edited.getPixel(target.first, target.second))
                            }
                        }
                    } finally {
                        edited.recycle()
                    }
                }
            }
        } finally {
            source.recycle()
        }
    }

    @Test
    fun blackRedactionIsBakedIntoPixelsAndDoesNotChangeTheSource() {
        val source = sampleBitmap(100, 80)
        val original = source.getPixel(50, 40)
        val redaction =
            DraftRedaction(
                listOf(DraftPoint(.25f, .25f), DraftPoint(.75f, .75f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.RECTANGLE,
                .1f,
            )
        val edited = rasterizeImageEdit(source, DraftImageEdit(marks = listOf(redaction)))
        try {
            assertEquals(Color.BLACK, edited.getPixel(50, 40))
            assertEquals(source.getPixel(5, 5), edited.getPixel(5, 5))
            assertEquals(original, source.getPixel(50, 40))
        } finally {
            edited.recycle()
            source.recycle()
        }
    }

    @Test
    fun pixelRedactionChangesInsideButNotOutsideItsMask() {
        val source = sampleBitmap(120, 80)
        val redaction =
            DraftRedaction(
                listOf(DraftPoint(.25f, .25f), DraftPoint(.75f, .75f)),
                DraftRedactionMode.PIXELS,
                DraftRedactionTool.RECTANGLE,
                .1f,
            )
        val edited = rasterizeImageEdit(source, DraftImageEdit(marks = listOf(redaction)))
        try {
            val changed = (30 until 90).any { x -> (20 until 60).any { y -> source.getPixel(x, y) != edited.getPixel(x, y) } }
            assertTrue(changed)
            assertEquals(source.getPixel(5, 5), edited.getPixel(5, 5))
        } finally {
            edited.recycle()
            source.recycle()
        }
    }

    @Test
    fun brushAndBannerAreActuallyDrawnIntoTheOutput() {
        val source = Bitmap.createBitmap(200, 120, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val stroke = DraftStroke(listOf(DraftPoint(.1f, .1f), DraftPoint(.9f, .1f)), Color.BLUE, .05f)
        val text = DraftTextMark("тест", DraftPoint(.5f, .5f), DraftTextAppearance(Color.RED, .2f, style = DraftTextStyle.BANNER))
        val edited = rasterizeImageEdit(source, DraftImageEdit(marks = listOf(stroke, text)))
        try {
            assertNotEquals(Color.WHITE, edited.getPixel(100, 12))
            assertTrue((40 until 160).any { x -> (35 until 85).any { y -> edited.getPixel(x, y) == Color.RED } })
        } finally {
            edited.recycle()
            source.recycle()
        }
    }

    @Test
    fun textStaysUprightAcrossRotationsAndReflections() {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        val label = DraftTextMark("Test", DraftPoint(.5f, .5f), DraftTextAppearance(Color.WHITE, .2f))
        val reference = rasterizeImageEdit(source, DraftImageEdit(marks = listOf(label)))
        try {
            assertTrue((0 until 100).any { x -> (0 until 100).any { y -> reference.getPixel(x, y) != Color.BLACK } })
            for (turns in 0..3) {
                for (mirror in listOf(false, true)) {
                    val edit = DraftImageEdit(quarterTurns = turns, mirrored = mirror, marks = listOf(label))
                    val rendered = rasterizeImageEdit(source, edit)
                    try {
                        for (y in 0 until 100) {
                            for (x in 0 until 100) assertEquals(reference.getPixel(x, y), rendered.getPixel(x, y))
                        }
                    } finally {
                        rendered.recycle()
                    }
                }
            }
        } finally {
            reference.recycle()
            source.recycle()
        }
    }

    @Test
    fun brushRedactionUsesOpaquePixelsAlongItsPath() {
        val source = sampleBitmap(100, 80)
        val mark =
            DraftRedaction(
                listOf(DraftPoint(.1f, .5f), DraftPoint(.9f, .5f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.BRUSH,
                .1f,
            )
        val edited = rasterizeImageEdit(source, DraftImageEdit(marks = listOf(mark)))
        try {
            assertEquals(Color.BLACK, edited.getPixel(50, 40))
            assertEquals(source.getPixel(50, 5), edited.getPixel(50, 5))
        } finally {
            edited.recycle()
            source.recycle()
        }
    }

    private fun sampleBitmap(
        width: Int,
        height: Int,
    ): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until height) {
                for (x in 0 until width) setPixel(x, y, Color.rgb(x % 256, y % 256, (x + y) % 256))
            }
        }

    private fun targetPixel(
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        turns: Int,
    ): Pair<Int, Int> =
        when (turns) {
            0 -> x to y
            1 -> height - 1 - y to x
            2 -> width - 1 - x to height - 1 - y
            else -> y to width - 1 - x
        }
}
