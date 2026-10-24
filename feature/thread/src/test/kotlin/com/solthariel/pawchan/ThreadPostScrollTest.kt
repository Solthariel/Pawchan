package com.solthariel.pawchan

import androidx.compose.foundation.lazy.LazyListState
import com.solthariel.pawchan.thread.scrollToThreadPost
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ThreadPostScrollTest {
    @Test
    fun unmeasuredListDoesNotStartAnAnimation() =
        runTest {
            val list = LazyListState()
            list.scrollToThreadPost(500)
            assertEquals(0, list.firstVisibleItemIndex)
            assertEquals(false, list.isScrollInProgress)
        }

    @Test
    fun negativeTargetIsIgnoredWithoutAFrameClock() =
        runTest {
            val list = LazyListState()
            list.scrollToThreadPost(-1)
            assertEquals(0, list.firstVisibleItemScrollOffset)
            assertEquals(false, list.isScrollInProgress)
        }
}
