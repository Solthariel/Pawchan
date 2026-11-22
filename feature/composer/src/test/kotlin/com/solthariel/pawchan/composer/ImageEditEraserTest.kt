package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import com.solthariel.pawchan.domain.drafts.DraftPoint
import com.solthariel.pawchan.domain.drafts.DraftRedaction
import com.solthariel.pawchan.domain.drafts.DraftRedactionMode
import com.solthariel.pawchan.domain.drafts.DraftRedactionTool
import com.solthariel.pawchan.domain.drafts.DraftShape
import com.solthariel.pawchan.domain.drafts.DraftShapeKind
import com.solthariel.pawchan.domain.drafts.DraftStroke
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ImageEditEraserTest {
    private val start = DraftPoint(.1f, .5f)
    private val end = DraftPoint(.9f, .5f)
    private val stroke = DraftStroke(listOf(start, end), 0xff000000.toInt(), .04f)

    @Test
    fun eraserRemovesOnlyTheTopmostCompleteDrawingMark() {
        val redaction = DraftRedaction(listOf(start, end), DraftRedactionMode.FILL, DraftRedactionTool.BRUSH, .1f)
        val edit = DraftImageEdit(marks = listOf(stroke, stroke, redaction))
        val result = eraseImageDrawing(edit, space(edit), ImageEditPoint(50f, 50f), 2f)
        assertEquals(listOf(stroke, redaction), result.marks)
        assertSame(edit, eraseImageDrawing(edit, space(edit), ImageEditPoint(50f, 10f), 1f))
    }

    @Test
    fun rectangleAndOvalAreHitAtTheirOutlineNotTheirInterior() {
        for (kind in listOf(DraftShapeKind.RECTANGLE, DraftShapeKind.OVAL)) {
            val shape = DraftShape(DraftPoint(.2f, .2f), DraftPoint(.8f, .8f), 0xff000000.toInt(), .02f, kind)
            val edit = DraftImageEdit(marks = listOf(shape))
            assertSame(edit, eraseImageDrawing(edit, space(edit), ImageEditPoint(50f, 50f), 1f))
            assertEquals(emptyList<Any>(), eraseImageDrawing(edit, space(edit), ImageEditPoint(50f, 20f), 1f).marks)
        }
    }

    @Test
    fun rotationAndMirrorMoveHitTargetsTogetherWithTheImage() {
        val edit = DraftImageEdit(quarterTurns = 1, mirrored = true, marks = listOf(stroke))
        val result = eraseImageDrawing(edit, space(edit), ImageEditPoint(50f, 70f), 1f)
        assertEquals(emptyList<Any>(), result.marks)
    }

    private fun space(edit: DraftImageEdit) = ImageEditRasterSpace(100, 100, ImageEditBounds(0, 0, 100, 100), edit)
}
