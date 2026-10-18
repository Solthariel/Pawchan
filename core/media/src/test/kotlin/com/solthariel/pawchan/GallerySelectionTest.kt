package com.solthariel.pawchan

import com.solthariel.pawchan.media.MediaGallerySession
import com.solthariel.pawchan.media.video.VideoSpeed
import org.junit.Assert.assertEquals
import org.junit.Test

class GallerySelectionTest {
    @Test fun allCircleSelectsEverythingThenClearsIt() {
        val session = MediaGallerySession()
        session.toggle("a")
        session.selectAll(listOf("a", "b", "c"))
        assertEquals(setOf("a", "b", "c"), session.selected)
        session.selectAll(listOf("a", "b", "c"))
        assertEquals(emptySet<String>(), session.selected)
    }

    @Test fun speedButtonCyclesBackToNormal() {
        val order = generateSequence(VideoSpeed.NORMAL) { it.next() }.take(5).map { it.label }.toList()
        assertEquals(listOf("1x", "1.5x", "2x", "0.5x", "1x"), order)
    }
}
