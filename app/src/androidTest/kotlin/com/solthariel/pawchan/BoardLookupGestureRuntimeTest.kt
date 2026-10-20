package com.solthariel.pawchan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardLookupPanel
import com.solthariel.pawchan.boards.BoardLookupSource
import com.solthariel.pawchan.boards.prepareLookup
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.catalog.CatalogBoardLookupLayer
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardLookupGestureRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val visible = mutableStateOf(true)
    private val list = LazyListState()
    private var threadClicks = 0

    @Test
    fun oneOutsideSwipeDismissesPanelAndScrollsTheOriginalList() {
        show()
        compose.onRoot().performTouchInput {
            swipe(Offset(center.x, height * 0.85f), Offset(center.x, height * 0.45f), durationMillis = 500)
        }
        compose.onNodeWithTag("board-lookup-panel").assertDoesNotExist()
        compose.runOnIdle { assertTrue(list.firstVisibleItemIndex > 0 || list.firstVisibleItemScrollOffset > 0) }
        assertEquals(0, threadClicks)
    }

    @Test
    fun firstOutsideTapOnlyDismissesAndSecondTapCanOpenAThread() {
        show()
        compose.onRoot().performTouchInput { click(Offset(center.x, height * 0.65f)) }
        compose.onNodeWithTag("board-lookup-panel").assertDoesNotExist()
        assertEquals(0, threadClicks)
        compose.onRoot().performTouchInput { click(Offset(center.x, height * 0.65f)) }
        compose.runOnIdle { assertEquals(1, threadClicks) }
    }

    @Test
    fun swipingInsideTheQuickGroupDoesNotDismissOrScrollThreads() {
        show()
        compose.onNodeWithTag("board-lookup-favorites").performTouchInput { swipeUp() }
        compose.onNodeWithTag("board-lookup-panel").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(0, list.firstVisibleItemScrollOffset)
            assertEquals(0, list.firstVisibleItemIndex)
        }
    }

    @Test
    fun blankCellInsidePanelDoesNotDismissIt() {
        show(count = 6)
        compose.onNodeWithTag("board-lookup-favorites").performTouchInput {
            click(Offset(width * 0.9f, height * 0.75f))
        }
        compose.onNodeWithTag("board-lookup-panel").assertIsDisplayed()
        assertEquals(0, threadClicks)
    }

    private fun show(count: Int = 30) {
        val board = defaultBoardAddress()
        val source =
            BoardLookupSource(
                favorites = (0 until count).map { requireNotNull(BoardId.parse("f$it")) }.toSet(),
            )
        val prepared = source.prepareLookup(board, "")
        compose.setContent {
            MaterialTheme {
                val lookup =
                    CatalogBoardLookupBindings(visible.value, "", {}, {}, { visible.value = false }) {
                        BoardLookupPanel(prepared, source, {}, {})
                    }
                CatalogBoardLookupLayer(lookup) {
                    Column(Modifier.fillMaxSize()) {
                        Box(Modifier.fillMaxWidth().height(54.dp)) { Text("Toolbar") }
                        LazyColumn(Modifier.weight(1f).testTag("real-thread-list"), state = list) {
                            items(100) { index ->
                                Text(
                                    "Thread $index",
                                    Modifier.fillMaxWidth().height(48.dp).clickable { threadClicks++ },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
