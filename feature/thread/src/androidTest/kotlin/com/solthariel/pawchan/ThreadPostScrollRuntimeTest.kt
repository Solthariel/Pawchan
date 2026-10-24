package com.solthariel.pawchan

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.thread.scrollToThreadPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ThreadPostScrollRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var list: LazyListState
    private lateinit var scope: CoroutineScope
    private val count = mutableIntStateOf(1000)

    @Test
    fun nearbyPostAlignsWithTheTop() {
        showList()
        moveTo(3)
        compose.runOnIdle {
            assertEquals(3, list.firstVisibleItemIndex)
            assertEquals(0, list.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun distantPostAlignsWithoutScreenHeightThreshold() {
        showList()
        moveTo(800)
        compose.runOnIdle {
            assertEquals(800, list.firstVisibleItemIndex)
            assertEquals(0, list.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun backwardTransitionReachesEarlierPost() {
        showList()
        moveTo(800)
        moveTo(10)
        compose.runOnIdle { assertEquals(10, list.firstVisibleItemIndex) }
    }

    @Test
    fun lastPostStopsAtTheEndIncludingBottomPadding() {
        showList()
        moveTo(999)
        compose.runOnIdle {
            assertFalse(list.canScrollForward)
            val info = list.layoutInfo
            val last = info.visibleItemsInfo.last()
            assertEquals(999, last.index)
            assertEquals(info.viewportEndOffset - info.afterContentPadding, last.offset + last.size)
        }
    }

    @Test
    fun emptyListAndNegativeTargetAreNoOps() {
        count.intValue = 0
        showList()
        moveTo(100)
        moveTo(-1)
        compose.runOnIdle { assertEquals(0, list.firstVisibleItemIndex) }
    }

    @Test
    fun targetBeyondRemovedPostsClampsToTheRemainingEnd() {
        showList()
        compose.runOnIdle { count.intValue = 12 }
        compose.waitForIdle()
        moveTo(800)
        compose.runOnIdle {
            assertFalse(list.canScrollForward)
            assertEquals(
                11,
                list.layoutInfo.visibleItemsInfo
                    .last()
                    .index,
            )
        }
    }

    @Test
    fun listShrinkingDuringAnimationStillStopsAtTheRemainingEnd() {
        showList()
        compose.mainClock.autoAdvance = false
        lateinit var animation: Job
        compose.runOnIdle { animation = scope.launch { list.scrollToThreadPost(800) } }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { count.intValue = 12 }
        compose.mainClock.autoAdvance = true
        compose.waitUntil(timeoutMillis = 10_000) { animation.isCompleted }
        compose.runOnIdle {
            assertFalse(animation.isCancelled)
            assertFalse(list.canScrollForward)
            assertEquals(
                11,
                list.layoutInfo.visibleItemsInfo
                    .last()
                    .index,
            )
        }
    }

    @Test
    fun newerTransitionCancelsThePreviousAnimation() {
        showList()
        compose.mainClock.autoAdvance = false
        lateinit var first: Job
        lateinit var second: Job
        compose.runOnIdle { first = scope.launch { list.scrollToThreadPost(800) } }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle { second = scope.launch { list.scrollToThreadPost(20) } }
        compose.mainClock.autoAdvance = true
        compose.waitUntil(timeoutMillis = 10_000) { first.isCompleted && second.isCompleted }
        compose.runOnIdle {
            assertTrue(first.isCancelled)
            assertFalse(second.isCancelled)
            assertEquals(20, list.firstVisibleItemIndex)
        }
    }

    @Test
    fun userPriorityScrollCancelsAnAutomaticTransition() {
        showList()
        compose.mainClock.autoAdvance = false
        lateinit var automatic: Job
        lateinit var gesture: Job
        compose.runOnIdle { automatic = scope.launch { list.scrollToThreadPost(800) } }
        compose.mainClock.advanceTimeByFrame()
        compose.runOnIdle {
            gesture = scope.launch { list.scroll(MutatePriority.UserInput) { scrollBy(80f) } }
        }
        compose.mainClock.autoAdvance = true
        compose.waitUntil(timeoutMillis = 10_000) { automatic.isCompleted && gesture.isCompleted }
        compose.runOnIdle {
            assertTrue(automatic.isCancelled)
            assertFalse(gesture.isCancelled)
        }
    }

    private fun showList() {
        val heights = List(1000) { (72 + (it % 5) * 28).dp }
        compose.setContent {
            list = rememberLazyListState()
            scope = rememberCoroutineScope()
            LazyColumn(
                state = list,
                modifier = Modifier.fillMaxWidth().height(360.dp).testTag("thread-scroll-list"),
                contentPadding = PaddingValues(bottom = 48.dp),
            ) {
                items(count.intValue, key = { it }) { index ->
                    Box(Modifier.fillMaxWidth().height(heights[index]))
                }
            }
        }
        compose.waitForIdle()
    }

    private fun moveTo(index: Int) {
        lateinit var job: Job
        compose.runOnIdle { job = scope.launch { list.scrollToThreadPost(index) } }
        compose.waitUntil(timeoutMillis = 10_000) { job.isCompleted }
        compose.runOnIdle { assertFalse("Unexpected cancellation for target $index", job.isCancelled) }
    }
}
