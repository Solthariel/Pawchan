package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class FastScrollbarRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: LazyListState

    @Test
    fun bottomDragReachesTheRealEndIncludingPadding() {
        showList()
        dragToBottom()
        compose.runOnIdle { assertFalse(state.canScrollForward) }
    }

    @Test
    fun bottomDragReachesTheBottomOfATallLastRow() {
        showList(lastRowHeight = 1200.dp)
        dragToBottom()
        compose.runOnIdle { assertFalse(state.canScrollForward) }
    }

    @Test
    fun draggingBackUpReachesTheTrueStart() {
        showList()
        dragToBottom()
        compose.onNodeWithTag(SCROLLBAR_TAG).performTouchInput {
            down(bottomCenter)
            moveTo(topCenter, delayMillis = 400)
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertFalse(state.canScrollBackward)
            assertEquals(0, state.firstVisibleItemIndex)
            assertEquals(0, state.firstVisibleItemScrollOffset)
        }
    }

    @Test
    fun railHidesAfterAQuickTapWithoutChangingListWidth() {
        showList()
        compose.mainClock.autoAdvance = false
        val widthBefore =
            compose
                .onNodeWithTag(LIST_TAG)
                .fetchSemanticsNode()
                .size.width
        compose.onNodeWithTag(SCROLLBAR_TAG).performTouchInput {
            down(center)
            up()
        }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        val visibleRail = SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)
        compose.onNode(visibleRail).assertExists()
        compose.mainClock.advanceTimeBy(1500)
        compose.waitForIdle()
        compose.onNode(visibleRail).assertDoesNotExist()
        assertEquals(
            widthBefore,
            compose
                .onNodeWithTag(LIST_TAG)
                .fetchSemanticsNode()
                .size.width,
        )
    }

    private fun showList(lastRowHeight: Dp = 110.dp) {
        compose.setContent {
            MaterialTheme {
                state = rememberLazyListState()
                Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                    LazyColumn(
                        state = state,
                        modifier = Modifier.fillMaxSize().testTag(LIST_TAG),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(100, key = { it }, contentType = { "test-row" }) { index ->
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(if (index == 99) lastRowHeight else 110.dp),
                            )
                        }
                    }
                    PawchanFastScrollbar(
                        state = state,
                        modifier = Modifier.align(Alignment.CenterEnd).testTag(SCROLLBAR_TAG),
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun dragToBottom() {
        compose.onNodeWithTag(SCROLLBAR_TAG).performTouchInput {
            down(topCenter)
            moveTo(bottomCenter, delayMillis = 400)
            up()
        }
        compose.waitForIdle()
    }
}

private const val SCROLLBAR_TAG = "fast-scrollbar"
private const val LIST_TAG = "scrollbar-list"
