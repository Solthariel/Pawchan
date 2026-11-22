package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test

class UiOutlinedChoiceContrastRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun activeOutlineStaysVisibleWithoutChangingSurfaceInAllThemes() {
        val index = mutableIntStateOf(0)
        val schemes =
            listOf(
                lightColorScheme(primary = Color.Black, onSurfaceVariant = Color.Black, surfaceContainerLow = Color.White),
                darkColorScheme(primary = Color.White, onSurfaceVariant = Color.White, surfaceContainerLow = Color.DarkGray),
                darkColorScheme(primary = Color.White, onSurfaceVariant = Color.White, surfaceContainerLow = Color.Black),
                lightColorScheme(primary = Color(0xff6030a0), onSurfaceVariant = Color.DarkGray, surfaceContainerLow = Color.White),
            )
        compose.setContent {
            MaterialTheme(colorScheme = schemes[index.intValue]) {
                Column {
                    UiOutlinedChoice(true, true, Modifier.size(72.dp, 48.dp).testTag("active"), RectangleShape, {}) {}
                    UiOutlinedChoice(false, true, Modifier.size(72.dp, 48.dp).testTag("inactive"), RectangleShape, {}) {}
                }
            }
        }
        schemes.indices.forEach { theme ->
            compose.runOnIdle { index.intValue = theme }
            val active =
                compose
                    .onNodeWithTag("active")
                    .assertIsSelected()
                    .captureToImage()
                    .toPixelMap()
            val inactive = compose.onNodeWithTag("inactive").captureToImage().toPixelMap()
            assertEquals(active.width, inactive.width)
            assertEquals(active.height, inactive.height)
            assertEquals(active[active.width / 2, active.height / 2], inactive[inactive.width / 2, inactive.height / 2])
            val density =
                androidx.test.platform.app.InstrumentationRegistry
                    .getInstrumentation()
                    .targetContext.resources.displayMetrics.density
            val borderY = (1.5f * density).toInt().coerceAtLeast(1)
            assertNotEquals(active[active.width / 2, borderY], inactive[inactive.width / 2, borderY])
        }
    }
}
