package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftCrop
import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageEditDimensionsTest {
    @Test
    fun cropThenRotateThenScaleUsesOrientedDecodedDimensions() {
        val edit = DraftImageEdit(crop = DraftCrop(.25f, .25f, .75f, .75f), quarterTurns = 1, mirrored = true)
        val result = imageEditDimensions(800, 600, edit, DraftImageReencoding(scaleDivisor = 2))
        assertEquals(ImageEditBounds(200, 150, 600, 450), result.crop)
        assertEquals(150, result.width)
        assertEquals(200, result.height)
    }

    @Test
    fun tinyCropsStayAtLeastOnePixelAndKeepBoundsInsideTheBitmap() {
        val edit = DraftImageEdit(crop = DraftCrop(.999f, .999f, 1f, 1f))
        val result = imageEditDimensions(3, 5, edit, DraftImageReencoding(scalePercent = 1))
        assertEquals(ImageEditBounds(2, 4, 3, 5), result.crop)
        assertEquals(1, result.width)
        assertEquals(1, result.height)
    }

    @Test
    fun decodeSamplingIsPowerOfTwoAndRespectsTheMemoryBudget() {
        assertEquals(1, imageEditDecodeSample(4000, 3000))
        assertEquals(2, imageEditDecodeSample(4001, 3000))
        assertEquals(4, imageEditDecodeSample(4000, 3000, 1_500_000))
        for (size in listOf(1, 100, 4001, 10000, Int.MAX_VALUE)) {
            val sample = imageEditDecodeSample(size, size)
            assertTrue(sample > 0 && sample and (sample - 1) == 0)
            val side = (size.toLong() + sample - 1) / sample
            assertTrue(side * side <= 12_000_000L)
        }
    }

    @Test
    fun invalidDimensionsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { imageEditDecodeSample(0, 3) }
        assertThrows(IllegalArgumentException::class.java) { imageEditDecodeSample(Int.MAX_VALUE, Int.MAX_VALUE, 1) }
        assertThrows(IllegalArgumentException::class.java) { imageEditDimensions(1, -1, DraftImageEdit(), null) }
    }
}
