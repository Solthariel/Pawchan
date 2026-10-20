package com.solthariel.pawchan

import com.solthariel.pawchan.thread.threadSearchTarget
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreadSearchTargetTest {
    private val ids = listOf(10L, 30L, 50L, 90L)
    private val positions = ids.associateWith { it.toInt() }

    @Test fun nextAndPreviousStartFromTheVisiblePostInTheMiddle() {
        assertEquals(50L, threadSearchTarget(ids, positions, 40, 1))
        assertEquals(30L, threadSearchTarget(ids, positions, 40, -1))
    }

    @Test fun currentMatchingPostIsSkippedInEitherDirection() {
        assertEquals(50L, threadSearchTarget(ids, positions, 30, 1))
        assertEquals(10L, threadSearchTarget(ids, positions, 30, -1))
    }

    @Test fun navigationWrapsAtBothEnds() {
        assertEquals(10L, threadSearchTarget(ids, positions, 90, 1))
        assertEquals(90L, threadSearchTarget(ids, positions, 10, -1))
    }

    @Test fun missingPostsAreIgnoredAndViewportOrderWinsOverTheResultIndex() {
        assertEquals(50L, threadSearchTarget(ids.reversed() + 999L, positions, 40, 1))
        assertNull(threadSearchTarget(listOf(999L), positions, 40, 1))
    }
}
