package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.boards.BoardStatus
import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Rule
import org.junit.Test

class BoardsStatusUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun readOnlyBoardUsesAnAccessibleIconRatherThanEmojiText() {
        compose.setContent {
            MaterialTheme { BoardStatus(board(postingEnabled = false), postsPerHour = 0, unavailable = false) }
        }
        compose.onNodeWithContentDescription("Доска только для чтения").assertIsDisplayed()
        compose.onNodeWithText("🔒").assertDoesNotExist()
        compose.onNodeWithText("Доска только для чтения").assertDoesNotExist()
    }

    @Test
    fun unavailableBoardUsesAnAccessibleIconRatherThanEmojiText() {
        compose.setContent {
            MaterialTheme { BoardStatus(board(postingEnabled = true), postsPerHour = 10, unavailable = true) }
        }
        compose.onNodeWithContentDescription("Доска недоступна").assertIsDisplayed()
        compose.onNodeWithText("🔒").assertDoesNotExist()
        compose.onNodeWithText("Доска недоступна").assertDoesNotExist()
    }

    private fun board(postingEnabled: Boolean): BoardDirectoryEntry =
        BoardDirectoryEntry(
            address =
                BoardAddress(
                    site = requireNotNull(SiteId.parse("dvach")),
                    board = requireNotNull(BoardId.parse("test")),
                ),
            title = "Тест",
            category = "Разное",
            postsPerHour = 0,
            postingEnabled = postingEnabled,
        )
}
