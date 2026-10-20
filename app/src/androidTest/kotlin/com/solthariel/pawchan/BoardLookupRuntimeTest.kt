package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardLookupPanel
import com.solthariel.pawchan.boards.BoardLookupSource
import com.solthariel.pawchan.boards.prepareLookup
import com.solthariel.pawchan.catalog.CatalogBoardLookupBindings
import com.solthariel.pawchan.catalog.CatalogBoardLookupLayer
import com.solthariel.pawchan.catalog.CatalogBoardLookupToolbar
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BoardLookupRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val query = mutableStateOf("")
    private val visible = mutableStateOf(true)
    private var selected: BoardAddress? = null

    @Test
    fun quickRowsAreCompactAndGroupsScrollIndependently() {
        show()
        compose.onNodeWithTag("board-lookup-favorites").assertHeightIsEqualTo(96.dp)
        compose.onNodeWithTag("board-lookup-recent").assertHeightIsEqualTo(96.dp)
        compose.onNodeWithTag("board-lookup-favorites").performScrollToIndex(29)
        compose.onNodeWithContentDescription("/f29/ Favorite 29").assertIsDisplayed()
        compose.onNodeWithContentDescription("/r0/ Recent 0").assertIsDisplayed()
        compose.onNodeWithText("Избранное").assertDoesNotExist()
        compose.onNodeWithText("Недавние").assertDoesNotExist()
    }

    @Test
    fun searchUsesSingleLineRowsAndSelectingClosesPanel() {
        show()
        compose.onNodeWithContentDescription("Введите доску…").performTextInput("моби")
        compose.onNodeWithText("/mobi/ Мобильные устройства").assertIsDisplayed()
        compose.onNodeWithTag("board-lookup-results").assertHeightIsEqualTo(48.dp)
        compose.onNodeWithText("/mobi/ Мобильные устройства").performClick()
        compose.onNodeWithTag("board-lookup-panel").assertDoesNotExist()
        assertEquals(board("mobi"), selected)
    }

    @Test
    fun clearingQueryRestoresQuickRowsAndDoesNotMoveThreadContent() {
        show()
        val before = compose.onNodeWithTag("threads-anchor").fetchSemanticsNode().boundsInRoot
        compose.onNodeWithContentDescription("Введите доску…").performTextInput("нет такой доски")
        compose.onNodeWithText("Ничего не найдено").assertIsDisplayed()
        compose.onNodeWithContentDescription("Очистить поиск").performClick()
        compose.onNodeWithTag("board-lookup-favorites").assertIsDisplayed()
        assertEquals(before, compose.onNodeWithTag("threads-anchor").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithContentDescription("Закрыть выбор доски").performClick()
        compose.onNodeWithTag("board-lookup-panel").assertDoesNotExist()
    }

    @Test
    fun searchHasAtMostFourRowsAndScrollsForMore() {
        show()
        compose.onNodeWithContentDescription("Введите доску…").performTextInput("Favorite")
        compose.onNodeWithTag("board-lookup-results").assertHeightIsEqualTo(192.dp)
        compose.onNodeWithTag("board-lookup-results").performScrollToIndex(29)
        compose.onNodeWithText("/f9/ Favorite 9").assertIsDisplayed()
    }

    @Test
    fun tappingOutsideClosesWithoutSelectingABoard() {
        show()
        compose.onRoot().performTouchInput { click(Offset(center.x, height * 0.7f)) }
        compose.onNodeWithTag("board-lookup-panel").assertDoesNotExist()
        assertEquals(null, selected)
    }

    @Test
    fun exactlyFiveCodesFitTheFirstRow() {
        show()
        val first = compose.onNodeWithContentDescription("/f0/ Favorite 0").fetchSemanticsNode().boundsInRoot
        val fifth = compose.onNodeWithContentDescription("/f4/ Favorite 4").fetchSemanticsNode().boundsInRoot
        val sixth = compose.onNodeWithContentDescription("/f5/ Favorite 5").fetchSemanticsNode().boundsInRoot
        assertEquals(first.top, fifth.top, 0.1f)
        org.junit.Assert.assertTrue(sixth.top > first.top)
    }

    @Test
    fun fewerRecentBoardsDoNotExpandTheFavoriteGroup() {
        val data = source().copy(recent = listOf(board("r0")))
        show(data)
        compose.onNodeWithTag("board-lookup-favorites").assertHeightIsEqualTo(96.dp)
        compose.onNodeWithTag("board-lookup-recent").assertHeightIsEqualTo(48.dp)
    }

    private fun show(data: BoardLookupSource = source()) {
        val source = data
        compose.setContent {
            MaterialTheme {
                val lookup =
                    CatalogBoardLookupBindings(
                        visible.value,
                        query.value,
                        { query.value = it },
                        {},
                        { visible.value = false },
                    ) {
                        BoardLookupPanel(source.prepareLookup(board("f0"), query.value), source, {
                            selected = it
                            visible.value = false
                        }, {})
                    }
                CatalogBoardLookupLayer(lookup) {
                    Column {
                        Box(Modifier.fillMaxWidth().height(54.dp)) {
                            if (visible.value) CatalogBoardLookupToolbar(lookup)
                        }
                        Text("Thread list", Modifier.testTag("threads-anchor"))
                    }
                }
            }
        }
    }

    private fun source(): BoardLookupSource {
        val favorites = (0..29).map { board("f$it") }
        val recent = (0..29).map { board("r$it") }
        val boards =
            favorites.mapIndexed { index, address -> entry(address, "Favorite $index") } +
                recent.mapIndexed { index, address -> entry(address, "Recent $index") } +
                entry(board("mobi"), "Мобильные устройства")
        return BoardLookupSource(boards, favorites.map { it.board }.toSet(), favorites + recent)
    }

    private fun board(code: String) =
        BoardAddress(
            requireNotNull(SiteId.parse("test")),
            requireNotNull(BoardId.parse(code)),
        )

    private fun entry(
        board: BoardAddress,
        title: String,
    ) = BoardDirectoryEntry(board, title, "", 0, true)
}
