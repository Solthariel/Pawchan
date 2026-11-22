package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.composer.ComposerModal
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ComposerThemeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun modalUsesCurrentThemeSurfaceWithoutTonalTint() {
        val dark = darkColorScheme(surface = Color(0xff183247), surfaceContainerHigh = Color(0xff645243))
        val light = lightColorScheme(surface = Color(0xffd8e7f1), surfaceContainerHigh = Color(0xfff1dac4))
        val scheme = mutableStateOf(dark)
        compose.setContent {
            MaterialTheme(colorScheme = scheme.value) {
                ComposerModal({}) { Box(Modifier.width(100.dp).height(100.dp)) }
            }
        }
        for (next in listOf(dark, light)) {
            compose.runOnIdle { scheme.value = next }
            val pixels = compose.onNodeWithTag("composer-modal-surface").captureToImage().toPixelMap()
            val actual = pixels[pixels.width / 2, pixels.height / 2]
            assertEquals(next.surface.red, actual.red, 0.01f)
            assertEquals(next.surface.green, actual.green, 0.01f)
            assertEquals(next.surface.blue, actual.blue, 0.01f)
        }
    }
}
