package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import com.solthariel.pawchan.boards.BoardLookupBoard
import com.solthariel.pawchan.boards.BoardLookupGrid
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class BoardLookupColorRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun selectedCodeUsesOnSecondaryContainerRatherThanInheritedTextColor() {
        val colors = lightColorScheme(onSecondaryContainer = Color.Red, onSurface = Color.Blue)
        val site = requireNotNull(SiteId.parse("dvach"))
        val boards =
            listOf("test" to true, "other" to false).map { (code, selected) ->
                BoardLookupBoard(BoardAddress(site, requireNotNull(BoardId.parse(code))), code, selected)
            }
        compose.setContent {
            MaterialTheme(colorScheme = colors) {
                BoardLookupGrid(boards, 2, 1, {}, "lookup")
            }
        }
        assertTextColor("/test/", colors.onSecondaryContainer)
        assertTextColor("/other/", colors.onSurface)
    }

    private fun assertTextColor(
        text: String,
        expected: Color,
    ) {
        val results = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        assertEquals(
            expected,
            results
                .single()
                .layoutInput.style.color,
        )
    }
}
