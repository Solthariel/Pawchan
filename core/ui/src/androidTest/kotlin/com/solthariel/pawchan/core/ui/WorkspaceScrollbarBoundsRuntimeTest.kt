package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class WorkspaceScrollbarBoundsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun scrollbarStartsBelowToolbarAndEndsAboveSystemNavigation() {
        val topInset = mutableStateOf(80.dp)
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalTopOverlayInset provides topInset.value,
                    LocalScrollbarBottomInset provides 24.dp,
                ) {
                    val state = rememberLazyListState()
                    Box(Modifier.size(300.dp, 400.dp).testTag("workspace")) {
                        LazyColumn(Modifier.fillMaxSize().testTag("list"), state = state) {
                            items((1..40).toList()) { Box(Modifier.height(48.dp)) }
                        }
                        PawchanFastScrollbar(state, Modifier.align(Alignment.CenterEnd).testTag("scroll-rail"))
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("scroll-rail").performTouchInput {
            down(center)
            up()
        }
        compose.mainClock.advanceTimeBy(200)
        compose.waitForIdle()
        assertBounds(80f, 24f)
        compose.runOnIdle { topInset.value = 120.dp }
        compose.mainClock.advanceTimeByFrame()
        compose.waitForIdle()
        assertBounds(120f, 24f)
    }

    private fun assertBounds(
        topDp: Float,
        bottomDp: Float,
    ) {
        val root = compose.onNodeWithTag("workspace").fetchSemanticsNode().boundsInRoot
        val scrollbar =
            compose
                .onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
                .fetchSemanticsNode()
                .boundsInRoot
        val density = compose.density.density
        assertEquals(root.top + topDp * density, scrollbar.top, 1f)
        assertEquals(root.bottom - bottomDp * density, scrollbar.bottom, 1f)
        assertEquals(root.right, scrollbar.right, 1f)
    }
}
