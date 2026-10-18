package com.solthariel.pawchan

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.saveable.SaverScope
import com.solthariel.pawchan.media.GalleryFilter
import com.solthariel.pawchan.media.GallerySessionSaver
import com.solthariel.pawchan.media.MediaGallerySession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaGallerySessionTest {
    @Test fun selectionIsIndependentAndClearedWhenSelectionModeChanges() {
        val session = MediaGallerySession()
        session.toggleSelection()
        session.toggle("a")
        session.toggle("b")
        session.toggle("a")
        assertTrue(session.selecting)
        assertEquals(setOf("b"), session.selected)
        session.toggleSelection()
        assertFalse(session.selecting)
        assertTrue(session.selected.isEmpty())
    }

    @Test fun selectAllAndClearSelectionResetGalleryState() {
        val session = MediaGallerySession()
        session.toggleSelection()
        session.selectAll(listOf("a", "b", "a"))
        assertEquals(setOf("a", "b"), session.selected)
        session.clearSelection()
        assertFalse(session.selecting)
        assertTrue(session.selected.isEmpty())
    }

    @Test fun saverRestoresFilterSelectionAndScrollAnchor() {
        val session = MediaGallerySession(LazyListState(5, 17))
        session.filter = GalleryFilter.VIDEO
        session.toggleSelection()
        session.toggle("a")
        val scope =
            object : SaverScope {
                override fun canBeSaved(value: Any): Boolean = true
            }
        val saved = requireNotNull(with(GallerySessionSaver) { scope.save(session) })
        val restored = requireNotNull(GallerySessionSaver.restore(saved))
        assertEquals(GalleryFilter.VIDEO, restored.filter)
        assertEquals(setOf("a"), restored.selected)
        assertTrue(restored.selecting)
        assertEquals(5, restored.list.firstVisibleItemIndex)
        assertEquals(17, restored.list.firstVisibleItemScrollOffset)
    }
}
