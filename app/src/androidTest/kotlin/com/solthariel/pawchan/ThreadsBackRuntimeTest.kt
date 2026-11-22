package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.threads.ThreadsLinkActions
import com.solthariel.pawchan.threads.ThreadsPageActions
import com.solthariel.pawchan.threads.ThreadsScreen
import com.solthariel.pawchan.threads.ThreadsScreenActions
import com.solthariel.pawchan.threads.ThreadsScreenInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ThreadsBackRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun backReturnsToCatalogButFirstClosesSearch() {
        var backs = 0
        compose.setContent {
            MaterialTheme {
                ThreadsScreen(
                    input = ThreadsScreenInput(emptyList(), emptyList(), emptyList(), null, emptySet(), 0, false),
                    actions =
                        ThreadsScreenActions(
                            open = { _, _ -> },
                            pages = ThreadsPageActions({}, {}),
                            setFavorite = { _, _ -> },
                            dismissReplies = {},
                            copyLink = {},
                            refresh = {},
                            links = ThreadsLinkActions({}, {}),
                        ),
                    bottomActions = AppBottomActions({}, {}, {}, {}),
                    onBack = { backs++ },
                )
            }
        }
        compose.onNodeWithContentDescription("Назад").assertIsDisplayed()
        compose.onNodeWithContentDescription("Поиск").performClick()
        compose.onNode(hasText("Страницы") and hasClickAction()).assertDoesNotExist()
        compose.onNodeWithContentDescription("Закрыть поиск").performClick()
        compose.onNode(hasText("Страницы") and hasClickAction()).assertExists()
        compose.onNodeWithText("Треды").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, backs) }
        compose.onNodeWithContentDescription("Назад").performClick()
        compose.runOnIdle { assertEquals(1, backs) }
    }
}
