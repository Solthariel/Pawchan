package com.solthariel.pawchan.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewerImagePageKeyTest {
    @Test
    fun neighborAndSelectedGifHaveTheSameIdentity() {
        val before = viewerImagePageKeys("still", null, listOf("gif"), 0)
        val after = viewerImagePageKeys("gif", null, listOf("still"), 0)
        assertEquals(before.single { it.url == "gif" }, after.single { it.url == "gif" })
        assertEquals(before.single { it.url == "still" }, after.single { it.url == "still" })
    }

    @Test
    fun retryChangesOnlySelectedResourceIdentity() {
        val before = viewerImagePageKeys("gif", null, listOf("still"), 0)
        val after = viewerImagePageKeys("gif", null, listOf("still"), 1)
        assertNotEquals(before.first(), after.first())
        assertEquals(before.last(), after.last())
    }

    @Test
    fun duplicatesNeverStartTwoPaintersForTheSameResource() {
        val keys = viewerImagePageKeys("gif", "gif", listOf("gif", "still", "still"), 2)
        assertEquals(listOf(ViewerImagePageKey("gif", 2), ViewerImagePageKey("still")), keys)
    }

    @Test
    fun thumbnailOnlyNeighborCanBecomeSelectedFallbackWithoutRestart() {
        val before = viewerImagePageKeys("still", "still-thumb", listOf("gif-thumb"), 0)
        val after = viewerImagePageKeys("gif", "gif-thumb", listOf("still-thumb"), 0)
        assertEquals(before.single { it.url == "gif-thumb" }, after.single { it.url == "gif-thumb" })
        assertTrue(before.none { it.url == "gif" })
    }

    @Test
    fun movingWindowDropsDistantResourcesAndKeepsImmediateNeighbor() {
        val before = viewerImagePageKeys("second", null, listOf("first", "third"), 0)
        val after = viewerImagePageKeys("third", null, listOf("second", "fourth"), 0)
        assertTrue(after.none { it.url == "first" })
        assertEquals(before.single { it.url == "third" }, after.first())
        assertEquals(3, after.size)
    }
}
