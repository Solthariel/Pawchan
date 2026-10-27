package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ReachableItemScrollbarRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var list: LazyListState
    private lateinit var session: ItemScrollSession

    @Test fun shortLastPostsHaveNoUnreachableTailAndMeasuringDoesNotChangePosition() {
        show(tallLast = false)
        verifyAnchors()
    }

    @Test fun tallLastPostHasASeparateReachableBottomPosition() {
        show(tallLast = true)
        verifyAnchors()
    }

    private fun verifyAnchors() {
        compose.runOnIdle { session.begin() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(0, list.firstVisibleItemIndex)
            assertEquals(0, list.firstVisibleItemScrollOffset)
        }
        lateinit var anchors: ItemScrollAnchors
        compose.runOnIdle { anchors = checkNotNull(session.anchors) }
        assertTrue(anchors.lastStep > 0)
        for (step in 0..anchors.lastStep) {
            val progress = step.toFloat() / anchors.lastStep
            compose.runOnIdle { session.request(progress) }
            compose.waitForIdle()
            compose.runOnIdle {
                val target = anchors.target(progress)
                assertEquals("step $step", target.index, list.firstVisibleItemIndex)
                assertEquals("offset $step", target.offset, list.firstVisibleItemScrollOffset)
            }
        }
        compose.runOnIdle { assertFalse(list.canScrollForward) }
        compose.runOnIdle { session.request(0f) }
        compose.waitForIdle()
        compose.runOnIdle { assertFalse(list.canScrollBackward) }
    }

    private fun show(tallLast: Boolean) {
        compose.setContent {
            MaterialTheme {
                list = rememberLazyListState()
                val scope = rememberCoroutineScope()
                session = remember(list, scope) { ItemScrollSession(list, scope) }
                LazyColumn(Modifier.height(400.dp), state = list) {
                    items(24, key = { it }) { index ->
                        val height =
                            if (tallLast && index == 23) {
                                1800.dp
                            } else if (index > 16) {
                                24.dp
                            } else {
                                120.dp
                            }
                        Box(Modifier.fillMaxWidth().height(height))
                    }
                }
            }
        }
        compose.waitForIdle()
    }
}
