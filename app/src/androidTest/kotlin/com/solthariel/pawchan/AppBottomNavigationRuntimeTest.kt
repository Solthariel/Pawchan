package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.navigation.chrome.AppBottomActions
import com.solthariel.pawchan.navigation.chrome.AppBottomBar
import com.solthariel.pawchan.navigation.chrome.AppBottomDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AppBottomNavigationRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun writeIsLastHas48DpTargetAndInvokesCurrentContextOnly() {
        var writes = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    AppBottomBar(AppBottomActions({}, {}, {}, {}, { writes++ }), AppBottomDestination.CATALOG)
                }
            }
        }
        val write = compose.onNodeWithContentDescription("Написать")
        write.assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, writes) }
        val bounds = write.fetchSemanticsNode().boundsInRoot
        assertTrue(bounds.height >= with(compose.density) { 48.dp.toPx() })
        assertTrue(bounds.width >= with(compose.density) { 48.dp.toPx() })
        assertTrue(
            bounds.left >=
                compose
                    .onNodeWithContentDescription("Настройки")
                    .fetchSemanticsNode()
                    .boundsInRoot.right,
        )
        compose.onNodeWithContentDescription("Каталог").assertIsSelected()
    }

    @Test fun unavailableWriteCannotReusePreviousContextAndTabsRemainEnabled() {
        var writes = 0
        var navigations = 0
        val available = mutableStateOf(true)
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    AppBottomBar(
                        AppBottomActions(
                            {},
                            {},
                            {},
                            { navigations++ },
                            if (available.value) ({ writes++ }) else null,
                        ),
                        AppBottomDestination.BOARDS,
                    )
                }
            }
        }
        compose.onNodeWithContentDescription("Написать").performClick()
        compose.runOnIdle { available.value = false }
        compose.onNodeWithContentDescription("Написать").assertIsNotEnabled().performClick()
        compose.onNodeWithContentDescription("Настройки").assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, writes)
            assertEquals(1, navigations)
        }
    }
}
