package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardCategoryUi
import com.solthariel.pawchan.boards.BoardsCategoryChips
import com.solthariel.pawchan.boards.BoardsSourceFilterRow
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardsCategoryChipsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun categorySelectionAndAllUseSeparateChips() {
        val board =
            BoardDirectoryEntry(
                BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
                "Бред",
                "Тематика",
                0,
                true,
            )
        val selected = mutableStateOf("")
        compose.setContent {
            MaterialTheme {
                BoardsCategoryChips(listOf(BoardCategoryUi("Тематика", listOf(board))), selected.value) {
                    selected.value = it
                }
            }
        }
        compose.onNodeWithText("Все").assertIsSelected()
        compose.onNodeWithText("Тематика").performClick().assertIsSelected()
        compose.onNodeWithText("Все").assertIsNotSelected()
        compose.runOnIdle { assertEquals("Тематика", selected.value) }
        compose.onNodeWithText("Все").performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("", selected.value) }
    }

    @Test fun stripUsesMatchingInsetsAndScrollsToTheLastChip() {
        val categories =
            listOf("Тематика", "Творчество", "Политика и новости", "Последняя")
                .map { BoardCategoryUi(it, emptyList()) }
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(320.dp)) {
                    BoardsSourceFilterRow { modifier ->
                        androidx.compose.foundation.layout
                            .Box(modifier.height(48.dp).testTag("source-region"))
                    }
                    BoardsCategoryChips(categories, "") { }
                }
            }
        }
        val source = compose.onNodeWithTag("source-region").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("board-category-chips").fetchSemanticsNode().boundsInRoot
        assertEquals(source.left, viewport.left, 1f)
        assertEquals(source.right, viewport.right, 1f)
        val first = compose.onNodeWithText("Все").fetchSemanticsNode().boundsInRoot
        assertTrue(first.left >= viewport.left && first.right <= viewport.right)
        compose.onNodeWithTag("board-category-chips").performScrollToNode(hasText("Последняя"))
        compose.onNodeWithText("Последняя").assertIsDisplayed()
        val last = compose.onNodeWithText("Последняя").fetchSemanticsNode().boundsInRoot
        assertTrue(last.left >= viewport.left && last.right <= viewport.right)
    }
}
