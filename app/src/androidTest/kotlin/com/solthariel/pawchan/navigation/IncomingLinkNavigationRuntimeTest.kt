package com.solthariel.pawchan.navigation

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.infrastructure.sites.ForumWebLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Rule
import org.junit.Test

class IncomingLinkNavigationRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var controller: NavHostController

    @Test fun aNewThreadLinkPreservesBackNavigation() {
        showNavigation()
        compose.runOnIdle {
            controller.openIncomingLink(ForumWebLink.Thread(defaultThreadAddress(123L), null), null)
            controller.openIncomingLink(ForumWebLink.Thread(defaultThreadAddress(789L), 800L), null)
            assertEquals(defaultThreadAddress(789L), controller.currentBackStackEntry?.arguments?.toThreadAddress())
            controller.popBackStack()
            assertEquals(defaultThreadAddress(123L), controller.currentBackStackEntry?.arguments?.toThreadAddress())
        }
    }

    @Test fun postLinkToAnAlreadyOpenThreadRecreatesTheReadingAnchor() {
        showNavigation()
        compose.runOnIdle {
            controller.navigateThread(defaultThreadAddress(123L))
            val before = controller.currentBackStackEntry
            controller.openIncomingLink(ForumWebLink.Thread(defaultThreadAddress(123L), 456L), null)
            assertNotSame(before, controller.currentBackStackEntry)
            controller.popBackStack()
            assertEquals(AppRoute.CATALOG, controller.currentBackStackEntry?.destination?.route)
        }
    }

    @Test fun boardLinkReplacesTheCatalogWithoutLeavingTheApp() {
        showNavigation()
        compose.runOnIdle {
            val board = requireNotNull(boardAddress("dvach", "mobi"))
            controller.openIncomingLink(ForumWebLink.Board(board), null)
            assertEquals(board, controller.currentBackStackEntry?.arguments?.toBoardAddress())
        }
    }

    private fun showNavigation() {
        compose.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = AppRoute.catalog(defaultBoardAddress())) {
                composable(AppRoute.CATALOG, arguments = boardArguments()) { Text("Каталог") }
                composable(AppRoute.THREAD, arguments = threadArguments()) { Text("Тред") }
            }
        }
        compose.waitForIdle()
    }
}
