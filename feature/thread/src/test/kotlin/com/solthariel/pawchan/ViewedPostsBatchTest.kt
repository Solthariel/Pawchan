package com.solthariel.pawchan

import com.solthariel.pawchan.thread.ThreadReadingSession
import org.junit.Assert.assertFalse
import org.junit.Test

/** Regression: scrolling must not expose a callback that counts visible posts. */
class ViewedPostsBatchTest {
    @Test fun readingSessionHasNoViewedPostsCollector() {
        assertFalse(ThreadReadingSession::class.java.methods.any { it.name == "viewed" })
    }
}
