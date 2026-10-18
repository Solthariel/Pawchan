package com.solthariel.pawchan

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.ui.FavoriteItemMenu
import com.solthariel.pawchan.ui.FavoriteMenuAction
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class FavoriteMenusRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun favoritesMenuHasOnlyOpenCopyAndRemove() {
        var action: FavoriteMenuAction? = null
        compose.setContent { FavoriteItemMenu({ action = it }, {}) }
        compose.onNodeWithText("Открыть тред").assertIsEnabled()
        compose.onNodeWithText("Копировать ссылку").assertIsEnabled()
        compose.onNodeWithText("Убрать тред из избранного").assertIsEnabled().performClick()
        compose.onNodeWithText("Ответить").assertDoesNotExist()
        compose.onNodeWithText("Цитировать").assertDoesNotExist()
        compose.onNodeWithText("Сохранить пост").assertDoesNotExist()
        compose.runOnIdle { assertEquals(FavoriteMenuAction.REMOVE, action) }
    }
}
