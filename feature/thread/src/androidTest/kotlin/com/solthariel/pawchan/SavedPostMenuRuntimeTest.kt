package com.solthariel.pawchan

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.thread.PostMenuCommand
import com.solthariel.pawchan.thread.PostMenuContent
import com.solthariel.pawchan.thread.PostMenuOptions
import com.solthariel.pawchan.thread.PostMenuPage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SavedPostMenuRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun postMenuHasNoRemovedFavoriteAction() {
        var command: PostMenuCommand? = null
        compose.setContent {
            PostMenuContent(
                PostMenuPage.MAIN,
                PostMenuOptions(true, false, false, false, false, hasLink = true),
                {},
                { command = it },
            )
        }
        compose.onNodeWithText("Пост в избранном").assertDoesNotExist()
        compose.onNodeWithText("Убрать пост из избранного").assertDoesNotExist()
        compose.runOnIdle { assertEquals(null, command) }
    }
}
