package com.solthariel.pawchan.navigation

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Реальный NavController: проверяет аргументы и порядок возврата, а не модель стека. */
class ThreadBackStackRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    private lateinit var controller: NavHostController

    @Test
    fun twentyThreadsReturnInReverseOrderBeforeCatalog() {
        showNavigation()
        compose.runOnIdle {
            for (id in 1L..20L) controller.navigateThread(defaultThreadAddress(id))
        }
        for (id in 20L downTo 1L) {
            compose.runOnIdle {
                assertEquals(defaultThreadAddress(id), controller.currentBackStackEntry?.arguments?.toThreadAddress())
                assertTrue(controller.popBackStack())
            }
        }
        compose.runOnIdle {
            assertEquals(AppRoute.CATALOG, controller.currentBackStackEntry?.destination?.route)
            assertFalse(controller.popBackStack())
        }
    }

    @Test
    fun openingCurrentThreadAgainDoesNotAddAnotherBackStep() {
        showNavigation()
        val address = defaultThreadAddress(1)
        compose.runOnIdle {
            controller.navigateThread(address)
        }
        compose.runOnIdle {
            val entry = controller.currentBackStackEntry
            repeat(3) { controller.navigateThread(address) }
            assertEquals(entry, controller.currentBackStackEntry)
            assertTrue(controller.popBackStack())
            assertEquals(AppRoute.CATALOG, controller.currentBackStackEntry?.destination?.route)
        }
    }

    @Test
    fun sameThreadNumberOnDifferentBoardsIsNotADuplicate() {
        showNavigation()
        val first = defaultThreadAddress(1)
        val second = requireNotNull(threadAddress("dvach", "vg", 1))
        compose.runOnIdle {
            controller.navigateThread(first)
        }
        compose.runOnIdle {
            controller.navigateThread(second)
        }
        compose.runOnIdle {
            assertEquals(second, controller.currentBackStackEntry?.arguments?.toThreadAddress())
            assertTrue(controller.popBackStack())
            assertEquals(first, controller.currentBackStackEntry?.arguments?.toThreadAddress())
        }
    }

    @Test
    fun openingAnEarlierThreadPreservesThePageBeingLeft() {
        showNavigation()
        val first = defaultThreadAddress(1)
        val second = defaultThreadAddress(2)
        compose.runOnIdle {
            controller.navigateThread(first)
        }
        compose.runOnIdle {
            controller.navigateThread(second)
        }
        compose.runOnIdle {
            controller.navigateThread(first)
        }
        compose.runOnIdle {
            assertTrue(controller.popBackStack())
            assertEquals(second, controller.currentBackStackEntry?.arguments?.toThreadAddress())
            assertTrue(controller.popBackStack())
            assertEquals(first, controller.currentBackStackEntry?.arguments?.toThreadAddress())
        }
    }

    private fun showNavigation() {
        compose.setContent {
            controller = rememberNavController()
            NavHost(controller, startDestination = AppRoute.catalog(defaultBoardAddress())) {
                composable(AppRoute.CATALOG, arguments = boardArguments()) {}
                composable(AppRoute.THREAD, arguments = threadArguments()) {}
            }
        }
        compose.waitForIdle()
    }
}
