package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.printToString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.test.espresso.Espresso
import com.solthariel.pawchan.navigation.AppAnimatedNavHost
import com.solthariel.pawchan.navigation.InstantNavigator
import com.solthariel.pawchan.navigation.appComposable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

/** Production host timing and state regression tests. */
class InstantNavigationProductionRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private lateinit var controller: NavHostController

    private fun setUpScreens() {
        compose.setContent {
            MaterialTheme {
                controller = rememberNavController(remember { InstantNavigator() })
                Box(Modifier.size(220.dp).testTag("pixel-host")) {
                    AppAnimatedNavHost(controller, "history") {
                        appComposable("history") {
                            Box(Modifier.fillMaxSize().background(Color.Red)) {
                                Button(
                                    onClick = { controller.navigate("thread") },
                                    modifier = Modifier.align(Alignment.BottomCenter).testTag("open"),
                                ) { Text("Open thread") }
                            }
                        }
                        appComposable("thread") {
                            Box(Modifier.fillMaxSize().background(Color.Green)) {
                                Button(
                                    onClick = { controller.popBackStack() },
                                    modifier = Modifier.align(Alignment.BottomCenter).testTag("back"),
                                ) { Text("Back to history") }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun firstFrameShowsOnlyDestinationColor() {
        setUpScreens()
        repeat(12) { iteration ->
            val forward = iteration % 2 == 0
            val before = if (forward) Color.Red else Color.Green
            val after = if (forward) Color.Green else Color.Red
            val route = if (forward) "thread" else "history"
            val initial = snapshot()
            assertTrue("Initial pixels do not match: $initial", matches(before, initial))
            compose.mainClock.autoAdvance = false
            val clockBefore = compose.mainClock.currentTime
            compose.onNodeWithTag(if (forward) "open" else "back").performClick()
            compose.runOnIdle { assertEquals(route, controller.currentDestination?.route) }
            compose.mainClock.advanceTimeByFrame()
            compose.waitForIdle()
            val first = snapshot()
            if (!matches(after, first)) {
                val next =
                    (1..3).joinToString("\n") { frame ->
                        compose.mainClock.advanceTimeByFrame()
                        compose.waitForIdle()
                        "diagnostic extra frame $frame: ${snapshot()}"
                    }
                throw AssertionError(
                    "Iteration $iteration ($route), clock before click=$clockBefore ms. " +
                        "Expected $after on first checked frame; $first\n$next",
                )
            }
            // Drain completed navigation before the next independent measurement.
            compose.mainClock.autoAdvance = true
            compose.waitForIdle()
        }
    }

    private fun snapshot(): FrameSnapshot {
        val beforeCapture = compose.mainClock.currentTime
        val pixels = compose.onNodeWithTag("pixel-host").captureToImage().toPixelMap()
        val samples = listOf(1, 2, 3).map { pixels[pixels.width * it / 4, pixels.height / 4] }
        val oldNodes = compose.onAllNodesWithText("Open thread").fetchSemanticsNodes().size
        val newNodes = compose.onAllNodesWithText("Back to history").fetchSemanticsNodes().size
        return FrameSnapshot(beforeCapture, compose.mainClock.currentTime, samples, oldNodes, newNodes)
    }

    private fun matches(
        expected: Color,
        snapshot: FrameSnapshot,
    ): Boolean =
        snapshot.samples.all { color ->
            abs(color.red - expected.red) < 0.03f &&
                abs(color.green - expected.green) < 0.03f &&
                abs(color.blue - expected.blue) < 0.03f
        }

    private data class FrameSnapshot(
        val clockBeforeCapture: Long,
        val clockAfterCapture: Long,
        val samples: List<Color>,
        val historyNodes: Int,
        val threadNodes: Int,
    )

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

    @Test
    fun draftArgumentsAndBackStackSurviveRoundTrip() {
        lateinit var nav: NavHostController
        compose.setContent {
            nav = rememberNavController(remember { InstantNavigator() })
            AppAnimatedNavHost(nav, "history") {
                appComposable("history") {
                    var draft by rememberSaveable { mutableStateOf("") }
                    Box(Modifier.size(220.dp)) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { draft = it },
                            modifier = Modifier.testTag("draft"),
                        )
                        Button(
                            onClick = {
                                nav.currentBackStackEntry?.savedStateHandle?.set("marker", "kept")
                                nav.navigate("thread/42")
                            },
                            modifier = Modifier.align(Alignment.BottomCenter).testTag("open-state"),
                        ) { Text("Open state") }
                    }
                }
                appComposable("thread/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                    Text("Thread ${entry.arguments?.getInt("id")}")
                }
            }
        }
        compose.onNodeWithTag("draft").performTextInput("kept draft")
        compose.onNodeWithTag("open-state").performClick()
        compose.onNodeWithText("Thread 42").assertIsDisplayed()
        compose.runOnIdle { assertEquals("history", nav.previousBackStackEntry?.destination?.route) }
        Espresso.pressBack()
        compose.onNodeWithTag("draft").assertTextContains("kept draft")
        compose.runOnIdle {
            assertEquals("history", nav.currentDestination?.route)
            assertEquals("kept", nav.currentBackStackEntry?.savedStateHandle?.get<String>("marker"))
        }
        compose.onNodeWithTag("open-state").performClick()
        compose.onNodeWithText("Thread 42").assertIsDisplayed()
        Espresso.pressBack()
        compose.onNodeWithTag("draft").assertTextContains("kept draft")
    }

    @Test
    fun backStackAndDraftSurviveSaveableStateRestoration() {
        val restoration = StateRestorationTester(compose)
        lateinit var nav: NavHostController
        restoration.setContent {
            nav = rememberNavController(remember { InstantNavigator() })
            AppAnimatedNavHost(nav, "history") {
                appComposable("history") {
                    var draft by rememberSaveable { mutableStateOf("") }
                    Column {
                        OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.testTag("restore-draft"))
                        Button(onClick = { nav.navigate("thread/42") }) { Text("Open restored thread") }
                    }
                }
                appComposable("thread/{id}", arguments = listOf(navArgument("id") { type = NavType.IntType })) { entry ->
                    Text("Restored thread ${entry.arguments?.getInt("id")}")
                }
            }
        }
        compose.onNodeWithTag("restore-draft").performTextInput("saved text")
        compose.onNodeWithText("Open restored thread").performClick()
        compose.onNodeWithText("Restored thread 42").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Restored thread 42").assertIsDisplayed()
        compose.runOnIdle { assertEquals("history", nav.previousBackStackEntry?.destination?.route) }
        Espresso.pressBack()
        compose.onNodeWithTag("restore-draft").assertTextContains("saved text")
    }

    @Test
    fun backKeepsPreviousViewModelAndClearsRemovedEntry() {
        lateinit var nav: NavHostController
        lateinit var first: NavigationProbeViewModel
        lateinit var second: NavigationProbeViewModel
        compose.setContent {
            nav = rememberNavController(remember { InstantNavigator() })
            AppAnimatedNavHost(nav, "history") {
                appComposable("history") {
                    first = viewModel()
                    Button(onClick = { nav.navigate("thread") }) { Text("Open probe") }
                }
                appComposable("thread") {
                    second = viewModel()
                    Text("Probe thread")
                }
            }
        }
        compose.waitForIdle()
        val initial = first
        compose.onNodeWithText("Open probe").performClick()
        compose.onNodeWithText("Probe thread").assertIsDisplayed()
        val removed = second
        Espresso.pressBack()
        compose.onNodeWithText("Open probe").assertIsDisplayed()
        compose.runOnIdle {
            assertSame(initial, first)
            assertTrue("Popped entry's ViewModel was not cleared", removed.cleared)
            assertTrue("Previous entry's ViewModel was cleared", !first.cleared)
        }
    }
}

class NavigationProbeViewModel : ViewModel() {
    var cleared: Boolean = false
        private set

    override fun onCleared() {
        cleared = true
    }
}
