package com.solthariel.pawchan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.solthariel.pawchan.thread.PostSelectionOwner
import org.junit.Assert.assertEquals
import org.junit.Test

class PostSelectionOwnerTest {
    @Test
    fun outsideTapClearsOnceAndInsideTapKeepsSelection() {
        val owner = PostSelectionOwner()
        var cleared = 0
        owner.select(Any(), { Rect(10f, 10f, 100f, 100f) }) { cleared++ }
        owner.outsideDown(Offset(20f, 20f))
        assertEquals(0, cleared)
        owner.outsideDown(Offset(5f, 5f))
        owner.outsideDown(Offset(5f, 5f))
        assertEquals(1, cleared)
    }

    @Test
    fun newSelectionClosesOldAndDisposedOldCannotClearNew() {
        val owner = PostSelectionOwner()
        val first = Any()
        val second = Any()
        var firstClosed = 0
        var secondClosed = 0
        owner.select(first, { Rect.Zero }) {
            firstClosed++
            owner.release(first)
        }
        owner.select(second, { Rect.Zero }) { secondClosed++ }
        assertEquals(1, firstClosed)
        owner.release(first)
        owner.outsideDown(Offset.Zero)
        assertEquals(1, secondClosed)
    }
}
