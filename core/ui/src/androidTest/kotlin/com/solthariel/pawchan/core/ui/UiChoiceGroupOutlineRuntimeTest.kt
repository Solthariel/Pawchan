package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class UiChoiceGroupOutlineRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun outerCornersDoNotClipSelectedChoiceOutline() {
        compose.setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color.Black, surfaceContainerLow = Color.White)) {
                Column(Modifier.background(Color.Magenta)) {
                    UiOutlinedChoice(true, true, Modifier.size(72.dp, 48.dp).testTag("reference"), onClick = {}) {}
                    Row(Modifier.uiChoiceGroup()) {
                        UiOutlinedChoice(true, true, Modifier.size(72.dp, 48.dp).testTag("left"), onClick = {}) {}
                        UiOutlinedChoice(true, true, Modifier.size(72.dp, 48.dp).testTag("right"), onClick = {}) {}
                    }
                }
            }
        }
        val reference = compose.onNodeWithTag("reference").captureToImage().toPixelMap()
        val left = compose.onNodeWithTag("left").captureToImage().toPixelMap()
        val right = compose.onNodeWithTag("right").captureToImage().toPixelMap()
        val cornerWidth = reference.width / 5
        val cornerHeight = reference.height / 2

        fun countDark(
            pixels: androidx.compose.ui.graphics.PixelMap,
            rightSide: Boolean,
        ): Int =
            (0 until cornerHeight).sumOf { y ->
                (0 until cornerWidth).count { x ->
                    val color = pixels[if (rightSide) pixels.width - 1 - x else x, y]
                    color.red < 0.35f && color.green < 0.35f && color.blue < 0.35f
                }
            }
        val expected = countDark(reference, false)
        assertTrue(expected > 0)
        assertTrue(countDark(left, false) >= expected * 0.8f)
        assertTrue(countDark(right, true) >= expected * 0.8f)
    }
}
