package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.arhivach.ArchiveBrowseFeedback
import com.solthariel.pawchan.arhivach.ArchiveBrowseState
import com.solthariel.pawchan.arhivach.ArchiveLoadState
import com.solthariel.pawchan.arhivach.ArchiveOpenThreadState
import com.solthariel.pawchan.core.ui.AppNoticeHost
import com.solthariel.pawchan.domain.archive.ArchiveThreadPage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArchiveRefreshFeedbackRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun initialErrorShowsRetryInsteadOfContent() {
        var retries = 0
        val state = ArchiveBrowseState(load = ArchiveLoadState(error = "Ошибка загрузки"))
        compose.setContent {
            MaterialTheme {
                AppNoticeHost {
                    ArchiveBrowseFeedback(state, { retries++ }) { Text("Содержимое") }
                }
            }
        }
        compose.onNodeWithText("Ошибка загрузки").assertIsDisplayed()
        compose.onNodeWithText("Содержимое").assertDoesNotExist()
        compose.onNodeWithText("Повторить").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }

    @Test fun refreshErrorKeepsContentAndShowsStandardNotice() {
        val state =
            ArchiveBrowseState(
                list = ArchiveThreadPage(emptyList(), hasNext = false, totalPages = 1),
                load = ArchiveLoadState(error = "Ошибка при загрузке данных"),
            )
        compose.setContent {
            MaterialTheme {
                AppNoticeHost {
                    ArchiveBrowseFeedback(state, {}) { Text("Список сохранён") }
                }
            }
        }
        compose.onNodeWithText("Список сохранён").assertIsDisplayed()
        compose.onNodeWithText("Ошибка при загрузке данных").assertIsDisplayed()
        compose.onNodeWithText("Повторить").assertDoesNotExist()
    }

    @Test fun oldListDoesNotMaskFailureToOpenDifferentThread() {
        val state =
            ArchiveBrowseState(
                list = ArchiveThreadPage(emptyList(), hasNext = false, totalPages = 1),
                openThread = ArchiveOpenThreadState(id = 55),
                load = ArchiveLoadState(error = "Ошибка загрузки треда"),
            )
        compose.setContent {
            MaterialTheme { ArchiveBrowseFeedback(state, {}) { Text("Не тот экран") } }
        }
        compose.onNodeWithText("Ошибка загрузки треда").assertIsDisplayed()
        compose.onNodeWithText("Не тот экран").assertDoesNotExist()
        compose.onNodeWithText("Повторить").assertIsDisplayed()
    }
}
