package com.solthariel.pawchan.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.catalog.CatalogBottomActions
import com.solthariel.pawchan.catalog.CatalogBottomDestination
import com.solthariel.pawchan.catalog.CatalogNavigationWorkspace
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

    private fun openThreadsChild(route: String) {
        compose.runOnIdle { controller.navigateTab(AppRoute.LIBRARY) }
        compose.runOnIdle { controller.navigate(route) }
    }

    private fun showNavigation() {
        compose.setContent {
            MaterialTheme {
                controller = rememberNavController()
                NavHost(controller, startDestination = AppRoute.catalog(defaultBoardAddress())) {
                    composable(AppRoute.CATALOG, arguments = boardArguments()) { Page("Каталог", null) }
                    composable(AppRoute.LIBRARY) { Page("Корень тредов", CatalogBottomDestination.THREADS) }
                    composable(AppRoute.HISTORY) { Page("История", CatalogBottomDestination.THREADS) }
                    composable(AppRoute.COPIES) { Page("Сохранённые копии", CatalogBottomDestination.THREADS) }
                    composable(AppRoute.SETTINGS) { Page("Корень настроек", CatalogBottomDestination.SETTINGS) }
                    composable(AppRoute.BOARDS) { Page("Доски", CatalogBottomDestination.BOARDS) }
                }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun Page(
        title: String,
        selected: CatalogBottomDestination?,
    ) {
        CatalogNavigationWorkspace(
            actions =
                CatalogBottomActions(
                    onBoardsClick = { controller.navigateTab(AppRoute.BOARDS) },
                    onCatalogClick = { controller.navigateCatalogBase() },
                    onLibraryClick = { controller.navigateTab(AppRoute.LIBRARY) },
                    onSettingsClick = { controller.navigateTab(AppRoute.SETTINGS) },
                ),
            selected = selected,
        ) { Text(title) }
    }
}
