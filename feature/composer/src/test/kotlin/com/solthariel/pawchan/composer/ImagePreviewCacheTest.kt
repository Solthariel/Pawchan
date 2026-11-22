package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.drafts.DraftImageEdit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImagePreviewCacheTest {
    @Test fun exactHistoryFramesAreAvailableWithoutRenderingAgain() {
        val cache = ImagePreviewCache<String>()
        val original = DraftImageEdit()
        val rotated = original.rotateLeft()
        cache.put(original, "original")
        cache.put(rotated, "rotated")
        assertEquals("original", cache[original])
        assertEquals("rotated", cache[rotated])
        assertNull(ImagePreviewCache<String>()[original])
    }

    @Test fun frameCountIsBoundedAndRefreshingAKeyDoesNotEvictIt() {
        val cache = ImagePreviewCache<Int>()
        val first = DraftImageEdit()
        val states = (0..3).map { first.copy(quarterTurns = it) }
        states.forEachIndexed { index, edit -> cache.put(edit, index) }
        cache.put(first, 4)
        cache.put(first.flipHorizontal(), 5)
        assertEquals(4, cache[first])
        assertNull(cache[states[1]])
        assertEquals(5, cache[first.flipHorizontal()])
    }
}
