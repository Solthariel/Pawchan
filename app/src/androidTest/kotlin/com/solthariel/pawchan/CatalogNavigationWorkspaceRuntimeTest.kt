package com.solthariel.pawchan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.catalog.CATALOG_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.catalog.CatalogBottomActions
import com.solthariel.pawchan.catalog.CatalogBottomDestination
import com.solthariel.pawchan.catalog.CatalogNavigationWorkspace
import com.solthariel.pawchan.core.ui.LocalPostDisplayPreferences
import com.solthariel.pawchan.core.ui.PawchanFastScrollbar
import com.solthariel.pawchan.core.ui.PostDisplayPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.roundToInt

class CatalogNavigationWorkspaceRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var state: LazyListState
    private var barHeightPx = 0f
    private var libraryClicks = 0

    @Test
    fun railEndsBesideTheBottomOfTheFloatingBubble() {
        showWorkspace()
        val body = compose.onNodeWithTag(BODY_TAG).fetchSemanticsNode().boundsInRoot
        val rail = compose.onNodeWithTag(RAIL_TAG).fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithContentDescription("Каталог").fetchSemanticsNode().boundsInRoot
        assertEquals(body.bottom, rail.bottom, 1f)
        assertEquals(body.bottom - barHeightPx / 2f, button.center.y, 1f)
    }

    @Test
    fun floatingBubbleHasTenDpInsetsOnBothSides() {
        showWorkspace()
        val body = compose.onNodeWithTag(BODY_TAG).fetchSemanticsNode().boundsInRoot
        val button = compose.onNodeWithContentDescription("Каталог").fetchSemanticsNode().boundsInRoot
        val pixels = compose.onNodeWithTag(BODY_TAG).captureToImage().toPixelMap()
        val row = (button.center.y - body.top).roundToInt().coerceIn(0, pixels.height - 1)
        val background = pixels[0, row]
        val left = (0 until pixels.width).first { pixels[it, row] != background }
        val right = (pixels.width - 1 downTo 0).first { pixels[it, row] != background }
        val expectedInset = barHeightPx * EXPECTED_INSET_DP / CATALOG_BOTTOM_BAR_HEIGHT.value
        assertEquals(expectedInset, left.toFloat(), 1f)
        assertEquals(expectedInset, (pixels.width - 1 - right).toFloat(), 1f)
    }

    @Test
    fun bottomTapReachesTheEndWithTheLastRowAboveTheButtons() {
        showWorkspace()
        tapRailBottom()
        compose.runOnIdle {
            assertFalse(state.canScrollForward)
            val layout = state.layoutInfo
            val last = layout.visibleItemsInfo.last()
            assertEquals(99, last.index)
            assertTrue(last.offset + last.size <= layout.viewportEndOffset - barHeightPx)
        }
    }

    @Test
    fun bottomBackingDoesNotCoverTheRailAndHideMovesItSideways() {
        showWorkspace()
        compose.mainClock.autoAdvance = false
        val background = railPixels()
        tapRailBottom()
        compose.mainClock.advanceTimeBy(250)
        compose.waitForIdle()
        val shown = railPixels()
        assertNotEquals(
            background[background.width - 2, background.height - 2],
            shown[shown.width - 2, shown.height - 2],
        )
        val row = shown.height / 3
        val base = shown[0, row]
        val first = (0 until shown.width).first { shown[it, row] != base }
        val trackColor = shown[shown.width - 1, row]
        var shifted = false
        repeat(14) {
            compose.mainClock.advanceTimeBy(100)
            compose.waitForIdle()
            val pixels = railPixels()
            val edge = (0 until pixels.width).firstOrNull { pixels[it, row] != base }
            if (edge != null && edge > first && edge < pixels.width - 1) {
                shifted = true
                // A visible rightmost pixel keeps its color: movement, not an alpha fade.
                assertEquals(trackColor, pixels[pixels.width - 1, row])
            }
        }
        assertTrue(shifted)
    }

    @Test
    fun floatingNavigationStillReceivesRealPointerClicks() {
        showWorkspace()
        compose.onNodeWithContentDescription("Треды").performTouchInput {
            down(center)
            up()
        }
        compose.runOnIdle { assertEquals(1, libraryClicks) }
    }

    private fun showWorkspace() {
        compose.setContent {
            MaterialTheme {
                CompositionLocalProvider(LocalPostDisplayPreferences provides PostDisplayPreferences(bubblePosts = true)) {
                    CatalogNavigationWorkspace(
                        actions = CatalogBottomActions({}, {}, { libraryClicks++ }, {}),
                        selected = CatalogBottomDestination.CATALOG,
                    ) {
                        WorkspaceContent()
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun WorkspaceContent() {
        state = rememberLazyListState()
        barHeightPx = with(LocalDensity.current) { CATALOG_BOTTOM_BAR_HEIGHT.toPx() }
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.fillMaxWidth().height(40.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth().testTag(BODY_TAG)) {
                LazyColumn(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = CATALOG_BOTTOM_BAR_HEIGHT + 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    workspaceRows()
                }
                PawchanFastScrollbar(
                    state = state,
                    modifier = Modifier.align(Alignment.CenterEnd).testTag(RAIL_TAG),
                )
            }
        }
    }

    private fun LazyListScope.workspaceRows() {
        items(100, key = { it }, contentType = { "workspace-test-row" }) {
            Box(modifier = Modifier.fillMaxWidth().height(110.dp))
        }
    }

    private fun tapRailBottom() {
        compose.onNodeWithTag(RAIL_TAG).performTouchInput {
            down(bottomCenter)
            up()
        }
        compose.waitForIdle()
    }

    private fun railPixels(): PixelMap = compose.onNodeWithTag(RAIL_TAG).captureToImage().toPixelMap()
}

private const val BODY_TAG = "floating-workspace-body"
private const val RAIL_TAG = "floating-workspace-rail"
private const val EXPECTED_INSET_DP = 10f
