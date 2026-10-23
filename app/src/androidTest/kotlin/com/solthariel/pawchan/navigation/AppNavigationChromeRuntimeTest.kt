package com.solthariel.pawchan.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.navigation.chrome.APP_BOTTOM_BAR_HEIGHT
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import com.solthariel.pawchan.navigation.chrome.AppNavigationWorkspace
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

class AppNavigationChromeRuntimeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun eachDestinationDispatchesItsOwnCallback() {
        val clicks = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                AppNavigationWorkspace(
                    actions =
                        AppBottomActions(
                            onBoardsClick = { clicks += "boards" },
                            onCatalogClick = { clicks += "catalog" },
                            onLibraryClick = { clicks += "threads" },
                            onSettingsClick = { clicks += "settings" },
                        ),
                    selected = null,
                ) {
                    Box(Modifier.testTag("navigation-content"))
                }
            }
        }
        listOf("Доски", "Каталог", "Треды", "Настройки").forEach {
            compose.onNodeWithContentDescription(it).performClick()
        }
        compose.onNodeWithTag("navigation-content").assertExists()
        compose.runOnIdle {
            assertEquals(listOf("boards", "catalog", "threads", "settings"), clicks)
        }
    }

    @Test
    fun contentReceivesTheNavigationOverlayInset() {
        var observed: Dp = (-1).dp
        compose.setContent {
            MaterialTheme {
                AppNavigationWorkspace(AppBottomActions({}, {}, {}, {}), AppBottomDestination.BOARDS) {
                    val inset = LocalBottomOverlayInset.current
                    SideEffect { observed = inset }
                }
            }
        }
        compose.runOnIdle { assertEquals(APP_BOTTOM_BAR_HEIGHT, observed) }
    }

    @Test
    fun changingSelectedDestinationPreservesTheContentSession() {
        val selected = mutableStateOf<AppBottomDestination?>(AppBottomDestination.CATALOG)
        var session: Any? = null
        compose.setContent {
            MaterialTheme {
                AppNavigationWorkspace(AppBottomActions({}, {}, {}, {}), selected.value) {
                    val owner = remember { Any() }
                    SideEffect { session = owner }
                }
            }
        }
        compose.waitForIdle()
        var original: Any? = null
        compose.runOnIdle {
            original = session
            selected.value = AppBottomDestination.THREADS
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertSame(original, session)
            selected.value = null
        }
        compose.waitForIdle()
        compose.runOnIdle { assertSame(original, session) }
    }
}
