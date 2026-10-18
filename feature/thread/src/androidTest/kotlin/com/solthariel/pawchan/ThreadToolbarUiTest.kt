package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.thread.ThreadActions
import com.solthariel.pawchan.thread.ThreadContentFlags
import com.solthariel.pawchan.thread.ThreadContentPresentation
import com.solthariel.pawchan.thread.ThreadContentStatistics
import com.solthariel.pawchan.thread.ThreadToolbar
import com.solthariel.pawchan.thread.ThreadToolsState
import com.solthariel.pawchan.thread.ThreadUiState
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

class ThreadToolbarUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun iconsReplaceTextLinksAndMenuDoesNotDuplicateThem() {
        compose.setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                val tools = remember { ThreadToolsState(scope) }
                val state =
                    ThreadUiState.Content(
                        1,
                        "Название",
                        emptyList(),
                        ThreadContentFlags(false),
                        ThreadContentPresentation(false),
                        ThreadContentStatistics(),
                    )
                ThreadToolbar(state, ThreadActions({}, {}, {}, {}), tools) {}
            }
        }
        for (description in listOf("Назад", "В избранное", "Поиск", "Написать", "Обновить", "Меню треда")) {
            compose.onNodeWithContentDescription(description).assertIsDisplayed()
        }
        compose.onNodeWithText("Название").assertDoesNotExist()
        compose.onNodeWithContentDescription("Меню треда").performClick()
        for (label in listOf(
            "Расширенный поиск",
            "Выделить",
            "Внешний вид",
            "Архивировать (HTML)",
        )) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
        for (label in listOf("Постов", "Файлов", "Размер файлов", "Участников")) {
            val row = compose.onNodeWithText(label)
            row.assertIsDisplayed()
            assertFalse(row.fetchSemanticsNode().config.contains(SemanticsActions.OnClick))
        }
        compose.onNodeWithText("Информация").assertDoesNotExist()
        compose.onNodeWithText("Сообщений с файлами").assertDoesNotExist()
        compose.onNodeWithText("Вложений-ссылок").assertDoesNotExist()
        compose.onNodeWithText("Поиск").assertDoesNotExist()
        compose.onNodeWithText("В избранное").assertDoesNotExist()
        compose.onNodeWithText("Расширенный поиск").performClick()
        compose.onNodeWithText("Мои посты").assertIsDisplayed()
        compose.onNodeWithText("С вложениями").assertIsDisplayed()
    }
}
