package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.domain.boards.BoardArchiveSnapshot
import com.solthariel.pawchan.library.ArchiveActions
import com.solthariel.pawchan.library.ArchiveScreen
import com.solthariel.pawchan.library.ArchiveUiState
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArchiveChromeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun archiveToolbarAndBottomPagerKeepTheirActions() {
        var refreshes = 0
        var back = 0
        var next = 0
        val actions = ArchiveActions({ back++ }, { refreshes++ }, {}, { next++ }, {})
        val state = ArchiveUiState(BoardArchiveSnapshot(emptyList(), (1..13).toList()), loading = false)
        compose.setContent { MaterialTheme { ArchiveScreen(defaultBoardAddress(), state, actions) } }
        compose.onNodeWithText("Архив /b/").assertExists()
        compose.onNodeWithText("1 из 13").assertExists()
        compose.onNodeWithText("Новее").assertIsNotEnabled()
        compose.onNodeWithText("Старее").performClick()
        compose.onNodeWithContentDescription("Обновить").performClick()
        compose.onNodeWithContentDescription("К каталогу").performClick()
        compose.runOnIdle {
            assertEquals(1, refreshes)
            assertEquals(1, back)
            assertEquals(1, next)
        }
    }
}
