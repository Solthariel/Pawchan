package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import com.solthariel.pawchan.boards.BoardsScreen
import com.solthariel.pawchan.boards.BoardsUiState
import com.solthariel.pawchan.catalog.BoardMenuNavigation
import com.solthariel.pawchan.catalog.BoardPreferenceActions
import com.solthariel.pawchan.catalog.BoardsActions
import com.solthariel.pawchan.catalog.CatalogMenuBindings
import com.solthariel.pawchan.catalog.CatalogOverflowMenu
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.settings.ThemePreference
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class BoardMenuRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun menuContainsOnlyRequestedActions() {
        showMenu(testBoardMenu())
        listOf(
            "Архив",
            "Новый тред",
            "Информация",
            "Внешний вид",
            "Добавить доску в избранное",
            "Сделать стартовой",
        ).forEach {
            compose.onNodeWithText(it).assertIsDisplayed()
        }
        listOf("Поиск", "Каталог", "Вид тредов", "Настройки").forEach {
            compose.onNodeWithText(it).assertDoesNotExist()
        }
    }

    @Test
    fun archiveDispatchesAndClosesTheMenu() {
        var opened = false
        showMenu(testBoardMenu(navigation = BoardMenuNavigation({ opened = true }, {}, {})))
        compose.onNodeWithText("Архив").performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithText("Архив").assertDoesNotExist()
    }

    @Test
    fun appearanceSelectsThemeWithoutOpeningSettings() {
        var selected: ThemePreference? = null
        showMenu(testBoardMenu(actions = BoardPreferenceActions({}, {}, { selected = it })))
        compose.onNodeWithText("Внешний вид").performClick()
        compose.onNodeWithText("AMOLED").performClick()
        compose.runOnIdle { assertEquals(ThemePreference.AMOLED, selected) }
    }

    @Test
    fun imeNavigatesToTypedBoardEvenBeforeDirectoryLoads() {
        var selected: BoardId? = null
        compose.setContent {
            MaterialTheme {
                BoardsScreen(BoardsUiState.Loading, { selected = it }, {}, BoardsActions({ _, _ -> }, { _, _ -> }, {}))
            }
        }
        compose.onNodeWithTag("boards-search").performTextInput("biz")
        compose.onNodeWithTag("boards-search").performImeAction()
        compose.runOnIdle { assertEquals("biz", selected?.value) }
    }

    @Test
    fun malformedImeInputDoesNotNavigate() {
        var selected: BoardId? = null
        compose.setContent {
            MaterialTheme {
                BoardsScreen(BoardsUiState.Loading, { selected = it }, {}, BoardsActions({ _, _ -> }, { _, _ -> }, {}))
            }
        }
        compose.onNodeWithTag("boards-search").performTextInput("biz po")
        compose.onNodeWithTag("boards-search").performImeAction()
        compose.runOnIdle { assertEquals(null, selected) }
        compose.onNodeWithText("Введите код доски, например biz или /biz/.").assertIsDisplayed()
    }

    private fun showMenu(menu: CatalogMenuBindings) {
        val expanded = mutableStateOf(false)
        compose.setContent {
            MaterialTheme { CatalogOverflowMenu(expanded.value, { expanded.value = it }, menu) }
        }
        compose.onNodeWithContentDescription("Другие действия").performClick()
    }
}
