package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class DraftCropHandleTest {
    @Test
    fun cornerDragUsesDisplaySpaceForEveryRotationAndMirror() {
        for (turn in 0..3) {
            for (mirrored in listOf(false, true)) {
                val edit = DraftImageEdit(DraftCrop(0.1f, 0.2f, 0.9f, 0.8f), turn, mirrored)
                val before = edit.cropOnFullCanvas()
                val target = ImageCanvasPoint(before.left + 0.1f, before.top + 0.1f)
                val after = edit.resizeCrop(DraftCropHandle.TOP_LEFT, target, 1000, 800).cropOnFullCanvas()
                assertEquals(target.x, after.left, DELTA)
                assertEquals(target.y, after.top, DELTA)
                assertEquals(before.right, after.right, DELTA)
                assertEquals(before.bottom, after.bottom, DELTA)
            }
        }
    }

    @Test
    fun freeHandleCannotCrossOppositeEdgeOrLeaveSource() {
        val edit = DraftImageEdit(DraftCrop(0.2f, 0.2f, 0.8f, 0.8f))
        val after = edit.resizeCrop(DraftCropHandle.TOP_LEFT, ImageCanvasPoint(2f, 3f), 100, 200)
        assertEquals(0.79f, after.crop.left, DELTA)
        assertEquals(0.795f, after.crop.top, DELTA)
        val expanded = edit.resizeCrop(DraftCropHandle.BOTTOM_RIGHT, ImageCanvasPoint(4f, 5f), 100, 200)
        assertEquals(1f, expanded.crop.right, DELTA)
        assertEquals(1f, expanded.crop.bottom, DELTA)
    }

    @Test
    fun fixedAspectCornerKeepsOppositeCornerAndCorrectPixelRatioAcrossTransforms() {
        for (turn in 0..3) {
            for (mirrored in listOf(false, true)) {
                val edit = DraftImageEdit(quarterTurns = turn, mirrored = mirrored)
                val after = edit.resizeCrop(DraftCropHandle.TOP_LEFT, ImageCanvasPoint(0.3f, 0.4f), 1600, 900, 1f)
                val crop = after.cropOnFullCanvas()
                val width = if (turn % 2 == 0) 1600f else 900f
                val height = if (turn % 2 == 0) 900f else 1600f
                assertEquals(1f, crop.right, DELTA)
                assertEquals(1f, crop.bottom, DELTA)
                assertEquals(1f, (crop.right - crop.left) * width / ((crop.bottom - crop.top) * height), DELTA)
            }
        }
    }

    @Test
    fun fixedAspectEdgeStaysCenteredAndInsideImage() {
        val edit = DraftImageEdit(DraftCrop(0.25f, 0.25f, 0.75f, 0.75f))
        val after = edit.resizeCrop(DraftCropHandle.RIGHT, ImageCanvasPoint(1f, 0f), 200, 100, 1f)
        val crop = after.cropOnFullCanvas()
        assertEquals(0.25f, crop.left, DELTA)
        assertEquals(0.5f, (crop.top + crop.bottom) / 2f, DELTA)
        assertEquals(1f, (crop.right - crop.left) * 200f / ((crop.bottom - crop.top) * 100f), DELTA)
        assertEquals(0f, crop.top, DELTA)
        assertEquals(1f, crop.bottom, DELTA)
    }

    @Test
    fun allHandlesStayInBoundsForManyPointerPositions() {
        val random = kotlin.random.Random(7)
        for (turn in 0..3) {
            for (mirrored in listOf(false, true)) {
                for (handle in DraftCropHandle.entries) {
                    repeat(80) {
                        val left = random.nextFloat() * 0.4f
                        val top = random.nextFloat() * 0.4f
                        val right = 0.6f + random.nextFloat() * 0.4f
                        val bottom = 0.6f + random.nextFloat() * 0.4f
                        val edit = DraftImageEdit(DraftCrop(left, top, right, bottom), turn, mirrored)
                        val point = ImageCanvasPoint(random.nextFloat() * 2f - 0.5f, random.nextFloat() * 2f - 0.5f)
                        val result = edit.resizeCrop(handle, point, 1600, 1200, 16f / 9f).cropOnFullCanvas()
                        val width = if (turn % 2 == 0) 1600f else 1200f
                        val height = if (turn % 2 == 0) 1200f else 1600f
                        val horizontalValid = result.left >= 0f && result.right <= 1f && result.left < result.right
                        val verticalValid = result.top >= 0f && result.bottom <= 1f && result.top < result.bottom
                        org.junit.Assert.assertTrue(horizontalValid && verticalValid)
                        val ratio = (result.right - result.left) * width / ((result.bottom - result.top) * height)
                        assertEquals(16f / 9f, ratio, 0.002f)
                    }
                }
            }
        }
    }

    @Test
    fun impossibleAspectLeavesCropUnchangedAndInvalidInputIsRejected() {
        val edit = DraftImageEdit()
        assertSame(edit, edit.resizeCrop(DraftCropHandle.RIGHT, ImageCanvasPoint(1f, 0f), 1, 1, 1000f))
        assertThrows(IllegalArgumentException::class.java) {
            edit.resizeCrop(DraftCropHandle.RIGHT, ImageCanvasPoint(Float.NaN, 0f), 100, 100)
        }
        assertThrows(IllegalArgumentException::class.java) {
            edit.resizeCrop(DraftCropHandle.RIGHT, ImageCanvasPoint(0f, 0f), 100, 100, 0f)
        }
    }
}

private const val DELTA = 0.00001f
