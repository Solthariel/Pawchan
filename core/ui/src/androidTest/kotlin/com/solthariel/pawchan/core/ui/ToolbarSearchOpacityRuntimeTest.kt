package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ToolbarSearchOpacityRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun searchBackgroundHasNinetyPercentOpacityRegardlessOfToolbarTransparency() {
        val transparency = mutableStateOf(0)
        val scheme = mutableStateOf(darkColorScheme())
        compose.setContent {
            MaterialTheme(colorScheme = scheme.value) {
                CompositionLocalProvider(
                    LocalFloatingTopEnabled provides true,
                    LocalTopBarTransparency provides transparency.value,
                ) {
                    FloatingTopChrome(
                        toolbar = {
                            UiToolbarSearchLayout(Modifier.width(240.dp).testTag("search"), onClear = null) {
                                Spacer(Modifier.weight(1f))
                            }
                        },
                    ) { Box(Modifier.fillMaxSize().background(Color.Magenta)) }
                }
            }
        }
        for (theme in listOf(darkColorScheme(), lightColorScheme())) {
            for (percent in listOf(0, 50, 100)) {
                compose.runOnIdle {
                    scheme.value = theme
                    transparency.value = percent
                }
                val panel = theme.background.copy(alpha = 1f - percent / 100f).compositeOver(Color.Magenta)
                val expected = theme.surfaceVariant.copy(alpha = 0.9f).compositeOver(panel)
                val pixels = compose.onNodeWithTag("search").captureToImage().toPixelMap()
                val actual = pixels[pixels.width / 2, pixels.height / 2]
                assertEquals(expected.red, actual.red, 0.02f)
                assertEquals(expected.green, actual.green, 0.02f)
                assertEquals(expected.blue, actual.blue, 0.02f)
            }
        }
    }
}
