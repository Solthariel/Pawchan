package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class DraftTextGestureTest {
    private val mark = DraftTextMark("Hello", DraftPoint(.5f, .5f), DraftTextAppearance(-1, .1f, angle = 170f))

    @Test
    fun translationAndRotationRemainInOutputCoordinatesForAllTransforms() {
        for (turn in 0..3) {
            for (mirror in listOf(false, true)) {
                val edit = DraftImageEdit(quarterTurns = turn, mirrored = mirror, marks = listOf(mark))
                val result = DraftTextGesture(edit, 0).update(ImageCanvasPoint(.1f, -.2f), 1.5f, 30f)
                val updated = result.marks.single() as DraftTextMark
                val canvas = result.sourceToCanvas(updated.anchor)
                assertEquals(.6f, canvas.x, DELTA)
                assertEquals(.3f, canvas.y, DELTA)
                assertEquals(.15f, updated.appearance.size, DELTA)
                assertEquals(-160f, updated.appearance.angle, DELTA)
                assertEquals(mark, edit.marks.single())
            }
        }
    }

    @Test
    fun scaleAndTranslationAreBoundedAndBadInputsAreRejected() {
        val edit = DraftImageEdit(marks = listOf(mark))
        val gesture = DraftTextGesture(edit, 0)
        val result = gesture.update(ImageCanvasPoint(5f, -5f), 100f, 0f).marks.single() as DraftTextMark
        assertEquals(1f, result.anchor.x, DELTA)
        assertEquals(0f, result.anchor.y, DELTA)
        assertEquals(1f, result.appearance.size, DELTA)
        assertThrows(IllegalArgumentException::class.java) { DraftTextGesture(edit, 1) }
        assertThrows(IllegalArgumentException::class.java) { gesture.update(ImageCanvasPoint(0f, 0f), 0f, 0f) }
        assertThrows(IllegalArgumentException::class.java) { gesture.update(ImageCanvasPoint(Float.NaN, 0f), 1f, 0f) }
    }
}

private const val DELTA = 0.00001f
