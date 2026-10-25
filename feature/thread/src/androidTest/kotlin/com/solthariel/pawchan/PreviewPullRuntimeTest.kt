package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.thread.LocalPreviewLayer
import com.solthariel.pawchan.thread.PreviewLayer
import com.solthariel.pawchan.thread.PreviewSwipeState
import com.solthariel.pawchan.thread.PreviewWindowFrame
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Real Compose pointer/nested-scroll wiring; execute on a device, not a JVM stand-in. */
class PreviewPullRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val motion = PreviewSwipeState()
    private val list = LazyListState()
    private var density = 1f
    private var popped = 0
    private var cleared = 0

    @Test fun shortFastPullRestoresInBothDirectionsInsteadOfClosing() {
        show(1)
        listOf(-1f, 1f).forEach { direction ->
            compose.onNodeWithTag("preview-active").performTouchInput {
                swipe(center, center + Offset(0f, direction * 30f * density), durationMillis = 60)
            }
            compose.waitForIdle()
            assertOpenAndRestored()
        }
    }

    @Test fun reachingTheListEdgeInTheSameGestureCanPullWithoutInstantDismissal() {
        show(50)
        compose.onNodeWithTag("preview-active").performTouchInput {
            swipe(center, center + Offset(0f, 30f * density), durationMillis = 60)
        }
        compose.waitForIdle()
        assertOpenAndRestored()
        compose.runOnIdle { assertEquals(0, list.firstVisibleItemIndex) }
    }

    @Test fun scrollingInsideLongRepliesDoesNotTranslateOrCloseTheWindow() {
        show(50)
        compose.onNodeWithTag("pull-test-list", useUnmergedTree = true).performTouchInput {
            swipe(center, center - Offset(0f, 150f * density), durationMillis = 400)
        }
        compose.waitForIdle()
        assertOpenAndRestored()
        compose.runOnIdle { assertTrue(list.firstVisibleItemIndex > 0) }
    }

    @Test fun deliberatePullBeyondShortCardStillClosesExactlyOneWindow() {
        show(1)
        val root = compose.onRoot().fetchSemanticsNode().boundsInRoot
        val window = compose.onNodeWithTag("preview-active").fetchSemanticsNode().boundsInRoot
        val start = window.center - root.topLeft
        val end = Offset(start.x, root.height * 0.05f)
        compose.onRoot().performTouchInput { swipe(start, end, durationMillis = 160) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, popped)
            assertEquals(0, cleared)
        }
    }

    private fun assertOpenAndRestored() {
        compose.onNodeWithTag("preview-active").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, popped)
            assertEquals(0, cleared)
            assertEquals(0f, motion.offset, 0.5f)
        }
    }

    private fun show(count: Int) {
        compose.setContent {
            density = LocalDensity.current.density
            MaterialTheme {
                Box(Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalPreviewLayer provides PreviewLayer(0, motion, { cleared++ })) {
                        PreviewWindowFrame({ popped++ }) {
                            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp).testTag("pull-test-list"), list) {
                                items((0 until count).toList()) { Text("Пост $it", Modifier.height(80.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}
