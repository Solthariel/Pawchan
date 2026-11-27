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
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Тред, открытый не из каталога, тоже остаётся точкой возврата: «Назад» с корня вкладки и «Каталог». */
class ReadingReturnRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private lateinit var controller: NavHostController

    @Test
    fun threadFromThreadsTabIsRememberedAfterSwitchingTabs() {
        showNavigation()
        openFromThreadsTab(101)
        compose.onNodeWithText("Позиция 0").performClick()
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithText("Корень настроек").assertIsDisplayed()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
        compose.onNodeWithText("Позиция 1").assertIsDisplayed()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Каталог").assertIsDisplayed()
    }

    @Test
    fun backFromThreadThenBackFromTheThreadsRootReturnsToTheThread() {
        showNavigation()
        openFromThreadsTab(101)
        compose.runOnIdle { assertTrue(controller.popReading()) }
        compose.onNodeWithText("Корень тредов").assertIsDisplayed()
        compose.runOnIdle { controller.navigateCatalogTab() }
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
    }

    @Test
    fun reselectingTheThreadsTabKeepsTheThreadForTheFirstButton() {
        showNavigation()
        openFromThreadsTab(101)
        compose.onNodeWithContentDescription("Треды").performClick()
        compose.onNodeWithText("Корень тредов").assertIsDisplayed()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
    }

    @Test
    fun theNewestThreadReplacesAnOlderCatalogThread() {
        showNavigation()
        compose.runOnIdle { controller.navigateThread(defaultThreadAddress(100)) }
        openFromThreadsTab(101)
        compose.onNodeWithContentDescription("Доски").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
        compose.runOnIdle { controller.popReading() }
        compose.onNodeWithText("Каталог").assertIsDisplayed()
    }

    @Test
    fun linkedThreadsInsideTheTabComeBackTogether() {
        showNavigation()
        openFromThreadsTab(101)
        compose.runOnIdle { controller.navigateThread(defaultThreadAddress(102)) }
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Тред 102").assertIsDisplayed()
        compose.runOnIdle { controller.popReading() }
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
    }

    @Test
    fun choosingABoardStillDropsTheThreadOpenedFromATab() {
        showNavigation()
        openFromThreadsTab(101)
        val board = requireNotNull(boardAddress("dvach", "vg"))
        compose.runOnIdle { controller.navigateBoard(board) }
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.runOnIdle {
            assertEquals(AppRoute.CATALOG, controller.currentBackStackEntry?.destination?.route)
            assertEquals(board, controller.currentBackStackEntry?.arguments?.toBoardAddress())
        }
    }

    @Test
    fun archiveFromDownloadsIsRememberedLikeAThread() {
        showNavigation()
        compose.runOnIdle {
            controller.navigateTab(AppRoute.LIBRARY)
            controller.navigate(AppRoute.downloadArchive(7))
        }
        compose.onNodeWithText("Архив 7").assertIsDisplayed()
        compose.onNodeWithContentDescription("Настройки").performClick()
        compose.onNodeWithContentDescription("Каталог").performClick()
        compose.onNodeWithText("Архив 7").assertIsDisplayed()
    }

    @Test
    fun backFromABoardOpenedByAPostLinkReturnsToTheThread() {
        showNavigation()
        compose.runOnIdle { controller.navigateThread(defaultThreadAddress(101)) }
        val board = requireNotNull(boardAddress("dvach", "vg"))
        compose.runOnIdle { controller.openLinkedBoard(board) }
        compose.onNodeWithText("Каталог").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(board, controller.currentBackStackEntry?.arguments?.toBoardAddress())
            assertTrue(controller.canReturnFromLinkedBoard())
            assertTrue(controller.returnFromLinkedBoard())
        }
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(defaultBoardAddress(), controller.previousBackStackEntry?.arguments?.toBoardAddress())
        }
    }

    @Test
    fun choosingABoardByHandClearsLinkReturns() {
        showNavigation()
        compose.runOnIdle {
            controller.navigateThread(defaultThreadAddress(101))
            controller.openLinkedBoard(requireNotNull(boardAddress("dvach", "vg")))
            controller.navigateBoard(requireNotNull(boardAddress("dvach", "po")))
        }
        compose.runOnIdle {
            assertFalse(controller.canReturnFromLinkedBoard())
            assertFalse(controller.returnFromLinkedBoard())
        }
    }

    @Test
    fun restartOnATabKeepsTheThreadThatWasRead() {
        showNavigation()
        compose.runOnIdle { controller.openRestoredRoute(AppRoute.LIBRARY, AppRoute.thread(defaultThreadAddress(101))) }
        compose.onNodeWithText("Корень тредов").assertIsDisplayed()
        compose.runOnIdle { controller.navigateCatalogTab() }
        compose.onNodeWithText("Тред 101").assertIsDisplayed()
    }

    private fun openFromThreadsTab(thread: Long) {
        compose.runOnIdle { controller.navigateTab(AppRoute.LIBRARY) }
        compose.runOnIdle { controller.navigateThread(defaultThreadAddress(thread)) }
        compose.onNodeWithText("Тред $thread").assertIsDisplayed()
    }

    private fun showNavigation() {
        compose.setContent {
            MaterialTheme {
                controller = rememberNavController(remember { InstantNavigator() })
                AppAnimatedNavHost(controller, startRoute = AppRoute.catalog(defaultBoardAddress())) {
                    appComposable(AppRoute.CATALOG, arguments = boardArguments()) { Page("Каталог", null) }
                    appComposable(AppRoute.THREAD, arguments = threadArguments()) { entry ->
                        var position by rememberSaveable { mutableIntStateOf(0) }
                        val thread = entry.arguments?.getLong(AppRoute.THREAD_ARGUMENT)
                        Page("Тред $thread", null) {
                            Button(onClick = { position++ }) { Text("Позиция $position") }
                        }
                    }
                    appComposable(
                        AppRoute.DOWNLOAD_ARCHIVE,
                        arguments = listOf(navArgument(DOWNLOAD_ARCHIVE_ARGUMENT) { type = NavType.LongType }),
                    ) { entry -> Page("Архив ${entry.arguments?.getLong(DOWNLOAD_ARCHIVE_ARGUMENT)}", null) }
                    appComposable(AppRoute.LIBRARY) { Page("Корень тредов", AppBottomDestination.THREADS) }
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
