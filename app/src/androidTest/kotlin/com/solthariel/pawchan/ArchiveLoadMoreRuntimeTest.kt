package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.arhivach.ArchiveBrowseState
import com.solthariel.pawchan.arhivach.ArchiveLoadMoreFooter
import com.solthariel.pawchan.arhivach.ArchivePaginationState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ArchiveLoadMoreRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun loadingIndicatorIsCenteredWithinFooter() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(300.dp).testTag("footer-container")) {
                    ArchiveLoadMoreFooter(ArchiveBrowseState(pagination = ArchivePaginationState(loadingNextPage = true)), true) {}
                }
            }
        }
        val parent = compose.onNodeWithTag("footer-container").fetchSemanticsNode().boundsInRoot
        val indicator =
            compose
                .onNodeWithContentDescription("Загрузка следующей страницы")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot
        assertEquals(parent.center.x, indicator.center.x, 1f)
    }

    @Test
    fun errorRetainsRetryAction() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                ArchiveLoadMoreFooter(ArchiveBrowseState(pagination = ArchivePaginationState(nextPageError = "Ошибка")), true) { retries++ }
            }
        }
        compose.onNodeWithText("Ошибка. Повторить").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, retries) }
        compose.onNodeWithContentDescription("Загрузка следующей страницы").assertDoesNotExist()
    }

    @Test
    fun searchModeRetainsManualLoadMore() {
        var requests = 0
        compose.setContent {
            MaterialTheme {
                ArchiveLoadMoreFooter(ArchiveBrowseState(), false) { requests++ }
            }
        }
        compose.onNodeWithText("Загрузить ещё").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, requests) }
    }
}
