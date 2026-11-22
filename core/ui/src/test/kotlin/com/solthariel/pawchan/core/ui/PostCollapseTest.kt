package com.solthariel.pawchan.core.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PostCollapseTest {
    @Test
    fun onlyUnexpandedOpeningPostUsesLineLimit() {
        assertEquals(10, postCollapseMaxLines(true, false, 10))
        assertEquals(30, postCollapseMaxLines(true, false, 30))
        assertEquals(Int.MAX_VALUE, postCollapseMaxLines(false, false, 10))
        assertEquals(Int.MAX_VALUE, postCollapseMaxLines(true, true, 10))
        assertEquals(Int.MAX_VALUE, postCollapseMaxLines(true, false, NO_POST_COLLAPSE))
    }
}
