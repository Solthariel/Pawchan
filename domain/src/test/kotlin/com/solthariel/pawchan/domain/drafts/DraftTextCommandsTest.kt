package com.solthariel.pawchan.domain.drafts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftTextCommandsTest {
    private val appearance = DraftTextAppearance(color = -1, size = .1f)
    private val position = ImageCanvasPoint(.3f, .7f)

    @Test
    fun addingAtCanvasPointSurvivesAllSourceTransformsAndUndo() {
        for (turn in 0..3) {
            for (mirror in listOf(false, true)) {
                val edit = DraftImageEdit(quarterTurns = turn, mirrored = mirror)
                val added = requireNotNull(edit.addTextMark(position, "Привет", appearance))
                val mark = added.marks.single() as DraftTextMark
                val actual = added.sourceToCanvas(mark.anchor)
                assertEquals(position.x, actual.x, .00001f)
                assertEquals(position.y, actual.y, .00001f)
                val history = DraftImageSession(edit)
                history.commit(added)
                history.undo()
                assertEquals(edit, history.current)
                history.redo()
                assertEquals(added, history.current)
            }
        }
    }

    @Test
    fun rejectsBlankLongInvalidPositionsAndMarkLimit() {
        val edit = DraftImageEdit()
        assertNull(edit.addTextMark(position, "  ", appearance))
        assertNull(edit.addTextMark(position, "a".repeat(MAX_IMAGE_TEXT_LENGTH + 1), appearance))
        assertNull(edit.addTextMark(ImageCanvasPoint(Float.NaN, 0f), "a", appearance))
        assertNull(edit.addTextMark(ImageCanvasPoint(-.1f, .5f), "a", appearance))
        val text = DraftTextMark("a", DraftPoint(.5f, .5f), appearance)
        val full = DraftImageEdit(marks = List(MAX_IMAGE_MARKS) { text })
        assertNull(full.addTextMark(position, "a", appearance))
    }

    @Test
    fun removingTextCannotRemoveRedactionOrOtherMarks() {
        val redaction =
            DraftRedaction(
                listOf(DraftPoint(.2f, .2f), DraftPoint(.7f, .7f)),
                DraftRedactionMode.FILL,
                DraftRedactionTool.RECTANGLE,
                .1f,
            )
        val edit = requireNotNull(DraftImageEdit(marks = listOf(redaction)).addTextMark(position, "a", appearance))
        assertTrue(edit.removeTextMark(0) === edit)
        assertTrue(edit.removeTextMark(-1) === edit)
        assertEquals(listOf(redaction), edit.removeTextMark(1).marks)
    }
}
