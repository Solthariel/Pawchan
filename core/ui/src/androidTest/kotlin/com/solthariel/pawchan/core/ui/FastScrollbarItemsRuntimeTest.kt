package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FastScrollbarItemsRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var list: LazyListState
    private var thumbHeight = 0f

    @Test fun slowDragAcrossMixedHeightPostsNeverMovesBackwards() {
        showList()
        val bar = compose.onNodeWithTag("items-bar")
        val height =
            bar
                .fetchSemanticsNode()
                .size.height
                .toFloat()
        val travel = height - thumbHeight
        val x = bar.fetchSemanticsNode().size.width / 2f
        val start = thumbHeight / 2f + travel * 0.59f
        bar.performTouchInput { down(Offset(x, start)) }
        compose.waitForIdle()
        var previous = 0
        repeat(80) { step ->
            bar.performTouchInput { moveTo(Offset(x, start + travel * step * 0.0001f), delayMillis = 32) }
            compose.waitForIdle()
            compose.runOnIdle {
                assertTrue("Reverse step $step", list.firstVisibleItemIndex >= previous)
                previous = list.firstVisibleItemIndex
            }
        }
        bar.performTouchInput { up() }
    }

    @Test fun grabbingTheThumbWithoutMovingPreservesThePartialPostOffset() {
        showList()
        compose.runOnIdle { runBlocking { list.scrollToItem(40, 12) } }
        compose.waitForIdle()
        val bar = compose.onNodeWithTag("items-bar")
        val height =
            bar
                .fetchSemanticsNode()
                .size.height
                .toFloat()
        val y = (height - thumbHeight) * (40f / 99f) + thumbHeight / 2f
        bar.performTouchInput {
            down(Offset(center.x, y))
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(40, list.firstVisibleItemIndex)
            assertEquals(12, list.firstVisibleItemScrollOffset)
        }
    }

    @Test fun bottomThenTopReachesBothRealEdgesWithTallLastPost() {
        showList()
        val bar = compose.onNodeWithTag("items-bar")
        bar.performTouchInput {
            down(topCenter)
            moveTo(bottomCenter, 400)
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle { assertFalse(list.canScrollForward) }
        bar.performTouchInput {
            down(bottomCenter)
            moveTo(topCenter, 400)
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle { assertFalse(list.canScrollBackward) }
    }

    @Test fun shortTailDoesNotReachTheEndSeveralPostStepsBeforeTheThumbDoes() {
        showList(tallLast = false)
        val bar = compose.onNodeWithTag("items-bar")
        val height =
            bar
                .fetchSemanticsNode()
                .size.height
                .toFloat()
        val travel = height - thumbHeight
        val x = bar.fetchSemanticsNode().size.width / 2f
        val positions = mutableListOf<Pair<Int, Int>>()
        bar.performTouchInput { down(Offset(x, thumbHeight / 2f + travel * 0.86f)) }
        compose.waitForIdle()
        for (progress in listOf(0.90f, 0.94f, 0.98f)) {
            bar.performTouchInput { moveTo(Offset(x, thumbHeight / 2f + travel * progress), 32) }
            compose.waitForIdle()
            compose.runOnIdle { positions += list.firstVisibleItemIndex to list.firstVisibleItemScrollOffset }
        }
        compose.runOnIdle {
            assertEquals(positions.size, positions.distinct().size)
            assertTrue("Thumb has not reached the bottom yet", list.canScrollForward)
        }
        bar.performTouchInput { up() }
    }

    private fun showList(tallLast: Boolean = true) {
        compose.setContent {
            MaterialTheme {
                list = rememberLazyListState()
                thumbHeight = with(LocalDensity.current) { 48.dp.toPx() }
                Box(Modifier.fillMaxSize()) {
                    LazyColumn(state = list, contentPadding = PaddingValues(bottom = 32.dp)) {
                        items(100, key = { it }) { index ->
                            Box(
                                Modifier.fillMaxWidth().height(
                                    when {
                                        index == 99 && tallLast -> 1800.dp
                                        !tallLast && index >= 90 -> 24.dp
                                        index % 3 == 0 -> 240.dp
                                        else -> 60.dp
                                    },
                                ),
                            )
                        }
                    }
                    PawchanFastScrollbar(
                        list,
                        Modifier.align(Alignment.CenterEnd).testTag("items-bar"),
                        mode = FastScrollMode.ITEMS,
                    )
                }
            }
        }
        compose.waitForIdle()
    }
}
