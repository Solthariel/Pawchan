package com.solthariel.pawchan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.solthariel.pawchan.core.ui.ModalLayerHost
import com.solthariel.pawchan.core.ui.ModalScrimStrength
import com.solthariel.pawchan.core.ui.ModalWindow
import com.solthariel.pawchan.domain.settings.ThemePreference
import com.solthariel.pawchan.ui.PawchanTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/** Run with visible Android navigation buttons; samples the themed background underneath them. */
class ModalNavigationBackgroundRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun normalDialogDimsTheThemesLastDrawnNavigationBackground() {
        checkBackground(ModalScrimStrength.STANDARD, 0.4f)
    }

    @Test fun mediaDialogDimsTheNavigationBackgroundToTenPercent() {
        checkBackground(ModalScrimStrength.MEDIA, 0.1f)
    }

    private fun checkBackground(
        strength: ModalScrimStrength,
        remaining: Float,
    ) {
        val open = mutableStateOf(false)
        var themeBackground = Color.Unspecified
        compose.setContent {
            ModalLayerHost {
                PawchanTheme(ThemePreference.LIGHT) {
                    themeBackground = MaterialTheme.colorScheme.background
                    Box(Modifier.fillMaxSize().background(Color.White)) {
                        TextButton({ open.value = true }) { Text("Open modal") }
                    }
                    if (open.value) {
                        ModalWindow({ open.value = false }, strength = strength) {
                            TextButton({ open.value = false }) { Text("Close modal") }
                        }
                    }
                }
            }
        }
        assertPixel(themeBackground, bottomPixel())
        compose.onNodeWithText("Open modal").performClick()
        assertPixel(
            Color(themeBackground.red * remaining, themeBackground.green * remaining, themeBackground.blue * remaining),
            bottomPixel(),
        )
        compose.onNodeWithText("Close modal").performClick()
        assertPixel(themeBackground, bottomPixel())
    }

    private fun bottomPixel(): Color {
        compose.waitForIdle()
        val pixels = compose.onNodeWithTag("modal-layer-host", useUnmergedTree = true).captureToImage().toPixelMap()
        return pixels[pixels.width / 2, pixels.height - 2]
    }

    private fun assertPixel(
        expected: Color,
        actual: Color,
    ) {
        assertEquals(expected.red, actual.red, 0.02f)
        assertEquals(expected.green, actual.green, 0.02f)
        assertEquals(expected.blue, actual.blue, 0.02f)
    }
}
