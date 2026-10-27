package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.library.ArchiveActions
import com.solthariel.pawchan.library.ArchiveScreen
import com.solthariel.pawchan.library.ArchiveThreadPresentation
import com.solthariel.pawchan.library.ArchiveUiState
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Rule
import org.junit.Test

class ArchivePresentationRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun archiveShowsRealSubjectNumberAndDateWithoutFakeCounters() {
        val board = defaultBoardAddress()
        val row = ArchiveThreadPresentation(ThreadAddress(board, ThreadId.parse(42)!!), "Архивная тема", "№42   01.01.26 10:30")
        compose.setContent {
            MaterialTheme {
                ArchiveScreen(
                    board,
                    ArchiveUiState(loading = false, rows = listOf(row)),
                    ArchiveActions({}, {}, {}, {}, {}),
                )
            }
        }
        compose.onNodeWithText("Архивная тема").assertExists()
        compose.onNodeWithText("№42   01.01.26 10:30").assertExists()
        compose.onNodeWithText("0 ответов").assertDoesNotExist()
    }
}
