package com.solthariel.pawchan

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import com.solthariel.pawchan.boards.BoardsAddressBindings
import com.solthariel.pawchan.boards.BoardsNavigation
import com.solthariel.pawchan.boards.BoardsScreen
import com.solthariel.pawchan.boards.BoardsUiState
import com.solthariel.pawchan.catalog.BoardMenuNavigation
import com.solthariel.pawchan.catalog.BoardPreferenceActions
import com.solthariel.pawchan.catalog.BoardsActions
import com.solthariel.pawchan.catalog.CatalogMenuBindings
import com.solthariel.pawchan.catalog.CatalogOverflowMenu
import com.solthariel.pawchan.core.ui.AppNoticeHost
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.infrastructure.sites.ForumCapabilities
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
        val menu = testBoardMenu(navigation = BoardMenuNavigation({ opened = true }, {}, {}))
        showMenu(menu.copy(capabilities = ForumCapabilities(boardArchive = true)))
        compose.onNodeWithText("Архив").assertIsEnabled().performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithText("Архив").assertDoesNotExist()
    }

    @Test
    fun arhivachIsSeparateAndBetweenArchiveAndNewThread() {
        var opened = false
        showMenu(testBoardMenu(navigation = BoardMenuNavigation({}, {}, {}, { opened = true })))
        val archive = compose.onNodeWithText("Архив").fetchSemanticsNode().boundsInRoot
        val arhivach = compose.onNodeWithText("Архивач").fetchSemanticsNode().boundsInRoot
        val newThread = compose.onNodeWithText("Новый тред").fetchSemanticsNode().boundsInRoot
        assertTrue(archive.top < arhivach.top && arhivach.top < newThread.top)
        compose.onNodeWithText("Архивач").performClick()
        compose.runOnIdle { assertTrue(opened) }
        compose.onNodeWithText("Архивач").assertDoesNotExist()
    }

    @Test
    fun archiveIsDisabledWhenSourceDoesNotSupportIt() {
        showMenu(testBoardMenu())
        compose.onNodeWithText("Архив").assertIsNotEnabled()
    }

    @Test
    fun appearanceSelectsThemeWithoutOpeningSettings() {
        var selected: ThemePreference? = null
        showMenu(testBoardMenu(actions = BoardPreferenceActions({}, {}, { selected = it })))
        compose.onNodeWithText("Внешний вид").performClick()
        compose.onNodeWithText("Тёмная").performClick()
        compose.runOnIdle { assertEquals(ThemePreference.DARK, selected) }
    }

    @Test
    fun imeNavigatesToTypedBoardEvenBeforeDirectoryLoads() {
        var selected: BoardAddress? = null
        compose.setContent {
            MaterialTheme {
                BoardsScreen(
                    BoardsUiState.Loading,
                    BoardsNavigation({ selected = it }, {}),
                    BoardsActions(
                        { _, _ -> },
                        { _, _ -> },
                        {},
                    ),
                    address = BoardsAddressBindings(requireNotNull(SiteId.parse("dvach")), { null }, {}),
                )
            }
        }
        compose.onNodeWithTag("boards-search").performTextInput("biz")
        compose.onNodeWithTag("boards-search").performImeAction()
        compose.runOnIdle { assertEquals("biz", selected?.board?.value) }
    }

    @Test
    fun malformedImeInputDoesNotNavigate() {
        var selected: BoardAddress? = null
        compose.setContent {
            MaterialTheme {
                AppNoticeHost {
                    BoardsScreen(
                        BoardsUiState.Loading,
                        BoardsNavigation({ selected = it }, {}),
                        BoardsActions(
                            { _, _ -> },
                            { _, _ -> },
                            {},
                        ),
                        address = BoardsAddressBindings(requireNotNull(SiteId.parse("dvach")), { null }, {}),
                    )
                }
            }
        }
        compose.onNodeWithTag("boards-search").performTextInput("biz po")
        compose.onNodeWithTag("boards-search").performImeAction()
        compose.runOnIdle { assertEquals(null, selected) }
        compose.onNodeWithText("Не удалось распознать доску или ссылку").assertIsDisplayed()
        compose.onNodeWithTag("app-notice").performClick().assertDoesNotExist()
        compose.onNodeWithTag("boards-search").performImeAction()
        compose.onNodeWithText("Не удалось распознать доску или ссылку").assertIsDisplayed()
        compose.runOnIdle { assertEquals(null, selected) }
        compose.onNodeWithTag("boards-search").performTextReplacement("biz")
        compose.onNodeWithTag("boards-search").performImeAction()
        compose.runOnIdle { assertEquals("biz", selected?.board?.value) }
    }

    private fun showMenu(menu: CatalogMenuBindings) {
        val expanded = mutableStateOf(false)
        compose.setContent {
            MaterialTheme { CatalogOverflowMenu(expanded.value, { expanded.value = it }, menu) }
        }
        compose.onNodeWithContentDescription("Другие действия").performClick()
    }
}
