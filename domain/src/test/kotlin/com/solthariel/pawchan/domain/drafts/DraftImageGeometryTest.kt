package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class DraftImageGeometryTest {
    @Test
    fun sourceCanvasRoundTripCoversAllEightTransformsAndCrop() {
        val point = DraftPoint(0.3f, 0.65f)
        for (turn in 0..3) {
            for (mirror in listOf(false, true)) {
                val edit = DraftImageEdit(DraftCrop(0.1f, 0.2f, 0.9f, 0.8f), turn, mirror)
                val restored = requireNotNull(edit.canvasToSource(edit.sourceToCanvas(point)))
                assertEquals(point.x, restored.x, 0.00001f)
                assertEquals(point.y, restored.y, 0.00001f)
            }
        }
    }

    @Test
    fun displaySpaceFlipsAndRotationFollowDihedralComposition() {
        val point = DraftPoint(0.2f, 0.7f)
        for (turn in 0..3) {
            for (mirror in listOf(false, true)) {
                val edit = DraftImageEdit(quarterTurns = turn, mirrored = mirror)
                val before = edit.sourceToCanvas(point)
                val horizontal = edit.flipHorizontal().sourceToCanvas(point)
                val vertical = edit.flipVertical().sourceToCanvas(point)
                val left = edit.rotateLeft().sourceToCanvas(point)
                assertEquals(1f - before.x, horizontal.x, 0.00001f)
                assertEquals(before.y, horizontal.y, 0.00001f)
                assertEquals(before.x, vertical.x, 0.00001f)
                assertEquals(1f - before.y, vertical.y, 0.00001f)
                assertEquals(before.y, left.x, 0.00001f)
                assertEquals(1f - before.x, left.y, 0.00001f)
            }
        }
    }

    @Test
    fun rejectsInvalidGeometryAndPointsOutsideSource() {
        assertThrows(IllegalArgumentException::class.java) { DraftCrop(right = 0f) }
        assertThrows(IllegalArgumentException::class.java) { DraftPoint(Float.NaN, 0f) }
        assertThrows(IllegalArgumentException::class.java) { DraftImageEdit(quarterTurns = 4) }
        assertNull(DraftImageEdit().canvasToSource(ImageCanvasPoint(-1f, 0.5f)))
    }
}
