package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftCropAspectTest {
    @Test
    fun aspectChoicesApplyInOutputCoordinatesForEveryRotationAndMirror() {
        val ratios = listOf(1f, 4f / 3, 3f / 4, 16f / 9, 9f / 16)
        for (turns in 0..3) {
            for (mirror in listOf(false, true)) {
                for (ratio in ratios) {
                    val start =
                        DraftImageEdit(
                            crop = DraftCrop(.1f, .2f, .9f, .8f),
                            quarterTurns = turns,
                            mirrored = mirror,
                        )
                    val result = start.cropToAspect(1600, 900, ratio)
                    val crop = result.crop
                    val width = (crop.right - crop.left) * 1600
                    val height = (crop.bottom - crop.top) * 900
                    val actual = if (turns % 2 == 0) width / height else height / width
                    assertEquals(ratio, actual, 0.0001f)
                    assertEquals(.5f, (crop.left + crop.right) / 2, 0.0001f)
                    assertEquals(.5f, (crop.top + crop.bottom) / 2, 0.0001f)
                    assertTrue(crop.left >= start.crop.left && crop.right <= start.crop.right)
                    assertTrue(crop.top >= start.crop.top && crop.bottom <= start.crop.bottom)
                }
            }
        }
    }

    @Test
    fun originalRatioTracksRotationButNotMirroring() {
        for (turns in 0..3) {
            val edit = DraftImageEdit(quarterTurns = turns, mirrored = true)
            val expected = if (turns % 2 == 0) 4f / 3 else 3f / 4
            assertEquals(expected, edit.originalOutputAspect(1600, 1200), 0.0001f)
            assertEquals(expected, edit.cropToAspect(1600, 1200, expected).originalOutputAspect(1600, 1200), 0.0001f)
        }
    }

    @Test
    fun invalidDimensionsAndAspectAreRejected() {
        val edit = DraftImageEdit()
        listOf(0f, Float.NaN, Float.POSITIVE_INFINITY, -1f).forEach { ratio ->
            try {
                edit.cropToAspect(1600, 900, ratio)
                throw AssertionError("Invalid aspect accepted: $ratio")
            } catch (_: IllegalArgumentException) {
                // Rejected by the domain boundary.
            }
        }
        try {
            edit.originalOutputAspect(0, 900)
            throw AssertionError("Invalid dimensions accepted")
        } catch (_: IllegalArgumentException) {
            // Rejected by the domain boundary.
        }
    }
}
