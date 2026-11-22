package com.solthariel.pawchan.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

/** Real bottom buttons and NavController: selected tabs must still receive taps. */
class TabReselectionRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var controller: NavHostController

    @Test
    fun tappingSelectedThreadsTabPopsHistory() {
        showNavigation()
        openThreadsChild(AppRoute.HISTORY)
        compose.onNodeWithText("История").assertIsDisplayed()
        compose.onNodeWithContentDescription("Треды").performClick()
        compose.onNodeWithText("Корень тредов").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppRoute.LIBRARY, controller.currentBackStackEntry?.destination?.route) }
    }

    @Test
    fun tappingSelectedThreadsTabPopsSavedCopies() {
        showNavigation()
        openThreadsChild(AppRoute.COPIES)
        compose.onNodeWithText("Сохранённые копии").assertIsDisplayed()
        compose.onNodeWithContentDescription("Треды").performClick()
        compose.onNodeWithText("Корень тредов").assertIsDisplayed()
        compose.runOnIdle { assertEquals(AppRoute.LIBRARY, controller.currentBackStackEntry?.destination?.route) }
    }

    @Test
    fun tappingTheRootAgainDoesNotRecreateItsBackStackEntry() {
        showNavigation()
        compose.runOnIdle { controller.navigateTab(AppRoute.LIBRARY) }
        val entry = compose.runOnIdle { controller.currentBackStackEntry }
        repeat(3) { compose.onNodeWithContentDescription("Треды").performClick() }
        compose.runOnIdle { assertSame(entry, controller.currentBackStackEntry) }
    }

    @Test
    fun switchingToAnotherTabAndBackStillRestoresHistory() {
        showNavigation()
        openThreadsChild(AppRoute.HISTORY)
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithText("Корень настроек").assertIsDisplayed()
        compose.onNodeWithContentDescription("Треды").performClick()
        compose.onNodeWithText("История").assertIsDisplayed()
    }

    @Test
    fun switchingAwayFromThreadAndBackRestoresTheSameThreadAndItsBackPath() {
        showNavigation()
        compose.runOnIdle {
            controller.navigate(AppRoute.thread(defaultThreadAddress(101)))
            controller.navigate(AppRoute.thread(defaultThreadAddress(102)))
        }
        val entryId = compose.runOnIdle { controller.currentBackStackEntry?.id }
        compose.onNodeWithText("Тред 102").assertIsDisplayed()
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Тред 102").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(entryId, controller.currentBackStackEntry?.id)
            controller.popBackStack()
        }
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
        compose.runOnIdle { controller.popBackStack() }
        compose.onNodeWithText("Каталог").assertIsDisplayed()
    }

    @Test
    fun catalogThreadSurvivesSeveralTabSwitchesAndReselectionReturnsToCatalog() {
        showNavigation()
        compose.runOnIdle { controller.navigate(AppRoute.thread(defaultThreadAddress(101))) }
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithContentDescription("Треды").performClick()
        compose.onNodeWithContentDescription("Доски").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Каталог").assertIsDisplayed()
    }

    @Test
    fun changingBoardDiscardsSuspendedThread() {
        showNavigation()
        compose.runOnIdle { controller.navigate(AppRoute.thread(defaultThreadAddress(101))) }
        compose.onNodeWithContentDescription("Доски").performClick()
        val board = requireNotNull(boardAddress("dvach", "vg"))
        compose.runOnIdle { controller.navigateBoard(board) }
        compose.onNodeWithText("Каталог").assertIsDisplayed()
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.runOnIdle {
            assertEquals(AppRoute.CATALOG, controller.currentBackStackEntry?.destination?.route)
            assertEquals(board, controller.currentBackStackEntry?.arguments?.toBoardAddress())
        }
    }

    @Test
    fun catalogArchiveAndThreadBackPathIsRestored() {
        showNavigation()
        compose.runOnIdle {
            controller.navigate(AppRoute.archive(defaultBoardAddress()))
            controller.navigate(AppRoute.thread(defaultThreadAddress(101)))
        }
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
        compose.runOnIdle { controller.popBackStack() }
        compose.onNodeWithText("Архив").assertIsDisplayed()
    }

    @Test
    fun restoringThreadRetainsItsSaveableUiState() {
        showNavigation()
        compose.runOnIdle { controller.navigate(AppRoute.thread(defaultThreadAddress(101))) }
        compose.onNodeWithText("Позиция 0").performClick()
        compose.onNodeWithText("Позиция 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Позиция 1").assertIsDisplayed()
    }

    private fun openThreadsChild(route: String) {
        compose.runOnIdle { controller.navigateTab(AppRoute.LIBRARY) }
        compose.runOnIdle { controller.navigate(route) }
    }

    private fun showNavigation() {
        compose.setContent {
            MaterialTheme {
                controller = rememberNavController(remember { InstantNavigator() })
                AppAnimatedNavHost(controller, startRoute = AppRoute.catalog(defaultBoardAddress())) {
                    appComposable(AppRoute.CATALOG, arguments = boardArguments()) { Page("Каталог", null) }
                    appComposable(AppRoute.ARCHIVE, arguments = boardArguments()) {
                        Page("Архив", AppBottomDestination.CATALOG)
                    }
                    appComposable(AppRoute.THREAD, arguments = threadArguments()) { entry ->
                        var position by rememberSaveable { mutableIntStateOf(0) }
                        val thread = entry.arguments?.getLong(AppRoute.THREAD_ARGUMENT)
                        Page("Тред $thread", AppBottomDestination.CATALOG) {
                            Button(onClick = { position++ }) { Text("Позиция $position") }
                        }
                    }
                    appComposable(AppRoute.LIBRARY) { Page("Корень тредов", AppBottomDestination.THREADS) }
                    appComposable(AppRoute.HISTORY) { Page("История", AppBottomDestination.THREADS) }
                    appComposable(AppRoute.COPIES) { Page("Сохранённые копии", AppBottomDestination.THREADS) }
                    appComposable(AppRoute.SETTINGS) { Page("Корень настроек", AppBottomDestination.SETTINGS) }
                    appComposable(AppRoute.BOARDS) { Page("Доски", AppBottomDestination.BOARDS) }
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun Page(
        title: String,
        selected: AppBottomDestination?,
        content: @Composable () -> Unit = {},
    ) {
        AppNavigationWorkspace(
            actions =
                AppBottomActions(
                    onBoardsClick = { controller.navigateTab(AppRoute.BOARDS) },
                    onCatalogClick = { controller.navigateCatalogTab() },
                    onLibraryClick = { controller.navigateTab(AppRoute.LIBRARY) },
                    onSettingsClick = { controller.navigateTab(AppRoute.SETTINGS) },
                ),
            selected = selected,
        ) {
            Column {
                Text(title)
                content()
            }
        }
    }
}
