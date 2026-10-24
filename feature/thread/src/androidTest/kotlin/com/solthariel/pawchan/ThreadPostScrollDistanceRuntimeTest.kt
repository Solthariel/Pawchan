package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.abs

class ThreadPostScrollDistanceRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var list: LazyListState
    private lateinit var scope: CoroutineScope
    private lateinit var rowPixels: List<Int>

    @Test
    fun distantForwardAnimationStaysWithinOneViewportOfTarget() {
        showList()
        checkBoundedTransition(800)
    }

    @Test
    fun distantBackwardAnimationStaysWithinOneViewportOfTarget() {
        showList()
        compose.runOnIdle { scope.launch { list.scrollToItem(800) } }
        compose.waitForIdle()
        checkBoundedTransition(10)
    }

    @Test
    fun forwardTransitionHandlesPostTallerThanTheViewport() {
        showList(tallPost = 400)
        checkBoundedTransition(400)
    }

    @Test
    fun backwardTransitionHandlesPostTallerThanTheViewport() {
        showList(tallPost = 400)
        compose.runOnIdle { scope.launch { list.scrollToItem(800) } }
        compose.waitForIdle()
        checkBoundedTransition(400)
    }

    private fun showList(tallPost: Int = -1) {
        val heights = List(1000) { if (it == tallPost) 1200.dp else (72 + (it % 5) * 28).dp }
        compose.setContent {
            val density = LocalDensity.current
            rowPixels = heights.map { with(density) { it.roundToPx() } }
            list = rememberLazyListState()
            scope = rememberCoroutineScope()
            LazyColumn(state = list, modifier = Modifier.fillMaxWidth().height(360.dp)) {
                items(1000, key = { it }) { index ->
                    Box(Modifier.fillMaxWidth().height(heights[index]))
                }
            }
        }
        compose.waitForIdle()
    }

    private fun checkBoundedTransition(target: Int) {
        compose.mainClock.autoAdvance = false
        lateinit var animation: Job
        compose.runOnIdle { animation = scope.launch { list.scrollToThreadPost(target) } }
        compose.mainClock.advanceTimeByFrame()
        val destination = rowPixels.take(target).sum()
        var frames = 0
        while (!animation.isCompleted && frames < 120) {
            compose.runOnIdle {
                val position = rowPixels.take(list.firstVisibleItemIndex).sum() + list.firstVisibleItemScrollOffset
                val info = list.layoutInfo
                val viewport = info.viewportEndOffset - info.viewportStartOffset
                assertTrue("Animated path exceeds one viewport", abs(position - destination) <= viewport + 1)
            }
            compose.mainClock.advanceTimeByFrame()
            frames++
        }
        compose.runOnIdle {
            assertTrue("Transition did not finish in 120 virtual frames", animation.isCompleted)
            assertFalse(animation.isCancelled)
            assertEquals(target, list.firstVisibleItemIndex)
            assertEquals(0, list.firstVisibleItemScrollOffset)
        }
        compose.mainClock.autoAdvance = true
    }
}
