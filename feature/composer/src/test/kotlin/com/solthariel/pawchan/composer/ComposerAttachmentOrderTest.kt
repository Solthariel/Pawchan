package com.solthariel.pawchan.composer

import androidx.compose.ui.geometry.Offset
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerAttachmentOrderTest {
    private val grid = ComposerDropGrid(418f, 47f, 6f)

    private fun files(count: Int) =
        (0 until count).map {
            ComposerAttachment(ReplyAttachment("$it.png", "image/png", byteArrayOf(1)), id = "$it")
        }

    @Test fun movePreservesInterveningOrderAndIdentity() {
        val files = files(8)
        val moved = moveComposerItem(files, 0, 6)
        assertEquals(listOf("1", "2", "3", "4", "5", "6", "0", "7"), moved.map { it.id })
        assertSame(files[0], moved[6])
        assertEquals(files, moveComposerItem(moved, 6, 0))
    }

    @Test fun invalidMovesAreNoOps() {
        val files = files(4)
        assertSame(files, moveComposerItem(files, -1, 2))
        assertSame(files, moveComposerItem(files, 0, 4))
        assertSame(files, moveComposerItem(files, 2, 2))
    }

    @Test fun gridHandlesRowsRtlAndEmptyTrailingSlots() {
        assertEquals(0, grid.indexAt(50f, 20f, 8))
        assertEquals(7, grid.indexAt(370f, 75f, 8))
        assertEquals(4, grid.indexAt(370f, 75f, 5))
        assertEquals(3, grid.copy(rtl = true).indexAt(50f, 20f, 8))
        assertEquals(0, grid.copy(rtl = true).indexAt(370f, 20f, 8))
        assertNull(grid.indexAt(-1f, 20f, 8))
        assertNull(grid.indexAt(Float.NaN, 20f, 8))
        assertNull(grid.indexAt(20f, 120f, 8))
        assertNull(grid.indexAt(20f, 20f, 0))
    }

    @Test fun crossRowDragIsLocalUntilCommitted() {
        val files = files(8)
        val state = ComposerAttachmentDragState()
        assertTrue(state.begin(files, grid, Offset(50f, 20f)))
        state.move(grid, Offset(370f, 73f))
        assertEquals("0", state.arrange(files).last().id)
        assertEquals("0", files.first().id)
        state.finish()
        assertNull(state.id)
        assertSame(files, state.arrange(files))
    }

    @Test fun previewUsesLatestEditsAndRejectsChangedMembership() {
        val files = files(4)
        val state = ComposerAttachmentDragState()
        state.begin(files, grid, Offset(50f, 20f))
        state.move(grid, Offset(370f, 20f))
        val edited = files.map { it.copy(options = it.options.copy(removeFileName = true)) }
        assertSame(edited.first(), state.arrange(edited).last())
        val removed = edited.drop(1)
        assertFalse(state.matches(removed))
        assertSame(removed, state.arrange(removed))
    }
}
