package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftMarkGestureTest {
    private val first = DraftPoint(.1f, .2f)
    private val second = DraftPoint(.5f, .7f)
    private val last = DraftPoint(.8f, .9f)

    @Test
    fun strokesAndMarkerRemainOneMarkAcrossUpdates() {
        val original = DraftImageEdit().rotateLeft()
        val gesture = requireNotNull(DraftMarkGesture.begin(original, DraftMarkTool.Pen(-1, .1f, true), first))
        gesture.update(second)
        val result = gesture.update(last)
        val stroke = result.marks.single() as DraftStroke
        assertEquals(listOf(first, second, last), stroke.points)
        assertTrue(stroke.marker)
        assertEquals(original.quarterTurns, result.quarterTurns)
        assertTrue(original.marks.isEmpty())
    }

    @Test
    fun shapesAndRectanglesUseOnlyTheFirstAndLatestPoints() {
        val arrow = DraftMarkTool.Shape(-1, .05f, DraftShapeKind.ARROW)
        val shape = requireNotNull(DraftMarkGesture.begin(DraftImageEdit(), arrow, first))
        shape.update(second)
        assertEquals(DraftShape(first, last, -1, .05f, DraftShapeKind.ARROW), shape.update(last).marks.single())
        val tool = DraftMarkTool.Redact(.1f, DraftRedactionMode.FILL, DraftRedactionTool.RECTANGLE)
        val redact = requireNotNull(DraftMarkGesture.begin(DraftImageEdit(), tool, first))
        redact.update(second)
        val result = redact.update(last).marks.single() as DraftRedaction
        assertEquals(listOf(first, last), result.points)
        assertEquals(DraftRedactionMode.FILL, result.mode)
    }

    @Test
    fun budgetNeverDropsEarlierPointsOrSilentlyExceedsLimit() {
        val used = DraftStroke(List(MAX_IMAGE_POINTS - 2) { first }, -1, .1f)
        val edit = DraftImageEdit(marks = listOf(used))
        val gesture = requireNotNull(DraftMarkGesture.begin(edit, DraftMarkTool.Pen(-1, .1f), first))
        gesture.update(second)
        val result = gesture.update(last)
        assertEquals(listOf(first, second), (result.marks.last() as DraftStroke).points)
        assertTrue(gesture.limitReached)
        assertEquals(MAX_IMAGE_POINTS, result.marks.sumOf { it.pointCount })
        assertNull(DraftMarkGesture.begin(result, DraftMarkTool.Pen(-1, .1f), first))
    }

    @Test
    fun sessionUndoTreatsTheWholeStrokeAsOneAction() {
        val session = DraftImageSession(DraftImageEdit())
        val gesture = requireNotNull(DraftMarkGesture.begin(session.current, DraftMarkTool.Pen(-1, .1f), first))
        session.beginGesture()
        session.preview(gesture.update(second))
        session.preview(gesture.update(last))
        session.finishGesture()
        session.undo()
        assertTrue(session.current.marks.isEmpty())
        assertFalse(session.canUndo)
        session.redo()
        assertEquals(listOf(first, second, last), (session.current.marks.single() as DraftStroke).points)
    }
}
