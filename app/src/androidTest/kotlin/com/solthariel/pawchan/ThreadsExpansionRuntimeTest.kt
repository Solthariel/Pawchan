package com.solthariel.pawchan

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import com.solthariel.pawchan.catalog.CatalogBottomActions
import com.solthariel.pawchan.navigation.defaultThreadAddress
import com.solthariel.pawchan.threads.ThreadRowUi
import com.solthariel.pawchan.threads.ThreadsLinkActions
import com.solthariel.pawchan.threads.ThreadsPageActions
import com.solthariel.pawchan.threads.ThreadsScreen
import com.solthariel.pawchan.threads.ThreadsScreenActions
import com.solthariel.pawchan.threads.ThreadsScreenInput
import org.junit.Rule
import org.junit.Test

class ThreadsExpansionRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun oneHundredFavoritesRemainReachableAndCanCollapseAgain() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context
            .getSharedPreferences("threads_hub", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
        val rows = (1..100).map { ThreadRowUi(defaultThreadAddress(it.toLong()), "Тред $it") }
        compose.setContent {
            MaterialTheme {
                ThreadsScreen(
                    ThreadsScreenInput(emptyList(), rows, emptyList(), null, emptySet(), 0, false),
                    actions(),
                    CatalogBottomActions({}, {}, {}, {}),
                )
            }
        }
        compose.onNodeWithText("Тред 100").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Развернуть остальные (95)"))
        compose.onNodeWithText("Развернуть остальные (95)").performClick()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Тред 100"))
        compose.onNodeWithText("Тред 100").assertExists()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Свернуть"))
        compose.onNodeWithText("Свернуть").performClick()
        compose.onNodeWithText("Тред 100").assertDoesNotExist()
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Тред 1"))
        compose.onNodeWithText("Тред 1").assertExists()
    }

    private fun actions() =
        ThreadsScreenActions(
            open = { _, _ -> },
            pages = ThreadsPageActions({}, {}),
            setFavorite = { _, _ -> },
            dismissReplies = {},
            copyLink = {},
            refresh = {},
            links = ThreadsLinkActions({}, {}),
        )
}
