package com.solthariel.pawchan

import com.solthariel.pawchan.media.MediaBarState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaBarStateTest {
    @Test
    fun systemBarOverrideLivesUntilLastViewerDisposes() {
        val state = MediaBarState()
        val first = Any()
        val second = Any()
        assertFalse(state.active)
        state.acquire(first)
        state.acquire(second)
        state.release(first)
        assertTrue(state.active)
        state.release(first)
        assertTrue(state.active)
        state.release(second)
        assertFalse(state.active)
    }
}
