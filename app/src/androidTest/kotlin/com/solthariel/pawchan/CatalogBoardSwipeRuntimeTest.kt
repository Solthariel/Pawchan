package com.solthariel.pawchan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardSwipeDirection
import com.solthariel.pawchan.boards.forumSwipeGesture
import com.solthariel.pawchan.core.ui.BidirectionalPullRefreshBox
import com.solthariel.pawchan.core.ui.PullRefreshOptions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CatalogBoardSwipeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun horizontalSwipesOnScrollableCatalogMapLeftToNextAndRightToPrevious() {
        val directions = mutableListOf<BoardSwipeDirection>()
        showCatalogGestureZone(directions)

        compose.onNodeWithTag("catalog-swipe-zone").performTouchInput {
            swipe(
                start =
                    androidx.compose.ui.geometry
                        .Offset(width * 0.82f, height * 0.5f),
                end =
                    androidx.compose.ui.geometry
                        .Offset(width * 0.18f, height * 0.5f),
                durationMillis = 300,
            )
        }
        compose.onNodeWithTag("catalog-swipe-zone").performTouchInput {
            swipe(
                start =
                    androidx.compose.ui.geometry
                        .Offset(width * 0.18f, height * 0.5f),
                end =
                    androidx.compose.ui.geometry
                        .Offset(width * 0.82f, height * 0.5f),
                durationMillis = 300,
            )
        }
        compose.runOnIdle {
            assertEquals(
                listOf(BoardSwipeDirection.NEXT, BoardSwipeDirection.PREVIOUS),
                directions,
            )
        }
    }

    @Test
    fun verticalScrollGestureDoesNotNavigateBoards() {
        val directions = mutableListOf<BoardSwipeDirection>()
        showCatalogGestureZone(directions)

        compose.onNodeWithTag("catalog-swipe-zone").performTouchInput { swipeUp() }

        compose.runOnIdle { assertEquals(emptyList<BoardSwipeDirection>(), directions) }
    }

    private fun showCatalogGestureZone(directions: MutableList<BoardSwipeDirection>) {
        compose.setContent {
            MaterialTheme {
                BidirectionalPullRefreshBox(
                    refreshing = false,
                    onRefresh = {},
                    options = PullRefreshOptions(allowBottom = true),
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .forumSwipeGesture { directions += it }
                            .testTag("catalog-swipe-zone"),
                ) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        items(count = 30) { index ->
                            Text("Thread $index", Modifier.fillMaxWidth().height(64.dp))
                        }
                    }
                }
            }
        }
    }
}
