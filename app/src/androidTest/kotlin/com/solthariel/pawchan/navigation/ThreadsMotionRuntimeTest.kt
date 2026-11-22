package com.solthariel.pawchan.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class ThreadsMotionRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun childAndHubOverlapDuringForwardAndBackAndPoppedEntrySurvivesTransition() {
        lateinit var controller: NavHostController
        lateinit var child: NavBackStackEntry
        compose.mainClock.autoAdvance = false
        compose.setContent {
            controller = rememberNavController(remember { InstantNavigator() })
            AppAnimatedNavHost(controller, AppRoute.LIBRARY) {
                appComposable(AppRoute.LIBRARY) { Text("Threads hub") }
                appComposable(AppRoute.HISTORY) { Text("History child") }
            }
        }
        compose.mainClock.advanceTimeBy(250)
        compose.runOnIdle { controller.navigate(AppRoute.HISTORY) }
        compose.mainClock.advanceTimeBy(80)
        compose.onNodeWithText("Threads hub").assertExists()
        compose.onNodeWithText("History child").assertExists()
        compose.mainClock.advanceTimeBy(250)
        compose.runOnIdle {
            child = requireNotNull(controller.currentBackStackEntry)
            controller.popBackStack()
        }
        compose.mainClock.advanceTimeBy(80)
        compose.onNodeWithText("Threads hub").assertExists()
        compose.onNodeWithText("History child").assertExists()
        compose.runOnIdle { assertNotEquals(Lifecycle.State.DESTROYED, child.lifecycle.currentState) }
        compose.mainClock.advanceTimeBy(250)
        compose.runOnIdle { assertEquals(Lifecycle.State.DESTROYED, child.lifecycle.currentState) }
    }
}
