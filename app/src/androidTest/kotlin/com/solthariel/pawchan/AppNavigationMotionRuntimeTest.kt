package com.solthariel.pawchan

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.printToString
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.solthariel.pawchan.navigation.AppAnimatedNavHost
import com.solthariel.pawchan.navigation.InstantNavigator
import com.solthariel.pawchan.navigation.appComposable
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppNavigationMotionRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun destinationChangesImmediatelyWithoutScreenTransition() {
        lateinit var controller: NavHostController
        compose.setContent {
            MaterialTheme {
                controller = rememberNavController(remember { InstantNavigator() })
                AppAnimatedNavHost(controller, "history") {
                    appComposable("history") {
                        Button(onClick = { controller.navigate("thread") }) {
                            Text("Open thread")
                        }
                    }
                    appComposable("thread") {
                        Text("Thread screen")
                    }
                }
            }
        }

        // Settle the initial host before freezing frames for the navigation under test.
        compose.waitForIdle()
        compose.runOnIdle { assertEquals("history", controller.currentDestination?.route) }
        compose.mainClock.autoAdvance = false
        val clockBeforeClick = compose.mainClock.currentTime
        try {
            compose.onNodeWithText("Open thread").performClick()
            compose.runOnIdle { assertEquals("thread", controller.currentDestination?.route) }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()

            compose.onNodeWithText("Thread screen").assertExists().assertIsDisplayed()
            compose.onNodeWithText("Open thread").assertDoesNotExist()
        } catch (failure: AssertionError) {
            throw AssertionError(navigationDiagnostics(controller, clockBeforeClick, failure), failure)
        }
    }

    private fun navigationDiagnostics(
        controller: NavHostController,
        clockBeforeClick: Long,
        failure: AssertionError,
    ): String {
        val navigationState =
            runCatching {
                compose.runOnIdle {
                    val entries =
                        controller.visibleEntries.value.joinToString { entry ->
                            "${entry.destination.route}:${entry.lifecycle.currentState}"
                        }
                    "current=${controller.currentDestination?.route}, visibleEntries=[$entries]"
                }
            }.getOrElse { "Navigation snapshot unavailable: $it" }
        val semantics =
            runCatching { compose.onRoot(useUnmergedTree = true).printToString() }
                .getOrElse { "Compose tree unavailable: $it" }
        val originalClock = compose.mainClock.currentTime
        // These frames are inspected only after the assertion has failed. Their result is
        // diagnostic and cannot turn a failure into a pass.
        val laterFrames =
            runCatching {
                (1..3).joinToString("\n") { frame ->
                    compose.mainClock.advanceTimeByFrame()
                    compose.waitForIdle()
                    val oldCount = compose.onAllNodesWithText("Open thread").fetchSemanticsNodes().size
                    val newCount = compose.onAllNodesWithText("Thread screen").fetchSemanticsNodes().size
                    "extraFrame=$frame, clock=${compose.mainClock.currentTime} ms, " +
                        "oldNodes=$oldCount, newNodes=$newCount"
                }
            }.getOrElse { "Later-frame diagnostic unavailable: $it" }
        return """
            |Original assertion: ${failure.message}
            |Test clock before click: $clockBeforeClick ms
            |Test clock at diagnostic: $originalClock ms
            |Clock auto-advance: ${compose.mainClock.autoAdvance}
            |Navigation snapshot after assertion: $navigationState
            |Unmerged Compose tree after assertion:
            |$semantics
            |Diagnostic frames after the failed assertion (failure is not retried):
            |$laterFrames
            """.trimMargin()
    }
}
