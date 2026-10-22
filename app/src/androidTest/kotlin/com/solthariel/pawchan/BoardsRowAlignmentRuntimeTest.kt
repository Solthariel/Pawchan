package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.boards.BoardDirectoryRow
import com.solthariel.pawchan.boards.BoardGroupPosition
import com.solthariel.pawchan.boards.BoardListActions
import com.solthariel.pawchan.boards.BoardListItem
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BoardsRowAlignmentRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortAndLongBoardCodesKeepTheSameTitleStart() {
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(360.dp)) {
                    listOf("b" to "Название один", "poetry" to "Название два").forEach { (code, title) ->
                        Column(Modifier.height(72.dp)) {
                            BoardDirectoryRow(row(code, title), BoardListActions({}, { _, _ -> }))
                        }
                    }
                }
            }
        }
        val first = compose.onNodeWithText("Название один", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Название два", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(first.left, second.left, 0.1f)
        val short = compose.onNodeWithText("/b/", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val long = compose.onNodeWithText("/poetry/", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(short.left, long.left, 0.1f)
        assertEquals(short.width, long.width, 0.1f)
        assertEquals(textSize("/b/"), textSize("Название один"))
    }

    private fun textSize(text: String): androidx.compose.ui.unit.TextUnit {
        val results = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results
            .single()
            .layoutInput.style.fontSize
    }

    private fun row(
        code: String,
        title: String,
    ): BoardListItem.Board =
        BoardListItem.Board(
            key = code,
            entry =
                BoardDirectoryEntry(
                    address = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse(code))),
                    title = title,
                    category = "Разное",
                    postsPerHour = 0,
                    postingEnabled = true,
                ),
            favorite = false,
            postsPerHour = null,
            unavailable = false,
            position = BoardGroupPosition.SINGLE,
        )
}
