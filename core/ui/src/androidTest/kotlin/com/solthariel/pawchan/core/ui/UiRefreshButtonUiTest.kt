package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiRefreshButtonUiTest {
    @get:Rule val compose = createComposeRule()

    @Test fun usesCallerDescriptionAndClickAction() {
        var calls = 0
        compose.setContent { MaterialTheme { UiRefreshButton(false, { calls++ }, "Reload forum") } }
        compose.onNodeWithContentDescription("Reload forum").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    @Test fun callerCanDisableRefreshWhileAnUpdateIsRunning() {
        compose.setContent { MaterialTheme { UiRefreshButton(true, {}, "Обновить", enabled = false) } }
        compose.onNodeWithContentDescription("Обновить").assertIsNotEnabled()
    }

    @Test fun refreshingKeepsTheToolbarArrowInsteadOfReplacingItWithThePullBadge() {
        val colors = lightColorScheme(primary = Color.Red, onPrimary = Color.White, outline = Color.Blue)
        compose.setContent {
            MaterialTheme(colorScheme = colors) {
                Box(Modifier.background(Color.White)) {
                    UiRefreshButton(true, {}, "Обновить", enabled = false)
                }
            }
        }
        val pixels = compose.onNodeWithContentDescription("Обновить").captureToImage().toPixelMap()
        val red =
            (0 until pixels.height).sumOf { y ->
                (0 until pixels.width).count { x ->
                    val color = pixels[x, y]
                    color.red > 0.8f && color.green < 0.2f && color.blue < 0.2f
                }
            }
        val blue =
            (0 until pixels.height).sumOf { y ->
                (0 until pixels.width).count { x ->
                    val color = pixels[x, y]
                    color.blue > 0.8f && color.red < 0.2f && color.green < 0.2f
                }
            }
        assertEquals("Pull badge must not replace toolbar arrow", 0, red)
        assertTrue("Toolbar arrow must remain visible", blue > 0)
    }

    @Test fun completedRefreshEnablesTheButtonAndPreservesItsClick() {
        val refreshing = mutableStateOf(true)
        var calls = 0
        compose.setContent {
            MaterialTheme { UiRefreshButton(refreshing.value, { calls++ }, "Обновить", enabled = !refreshing.value) }
        }
        compose.onNodeWithContentDescription("Обновить").assertIsNotEnabled()
        compose.runOnIdle { refreshing.value = false }
        compose.onNodeWithContentDescription("Обновить").assertIsEnabled().performClick()
        compose.runOnIdle { assertEquals(1, calls) }
    }
}
