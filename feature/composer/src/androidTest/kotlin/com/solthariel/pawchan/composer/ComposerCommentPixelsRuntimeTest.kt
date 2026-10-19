package com.solthariel.pawchan.composer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerCommentPixelsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    private var pixelsPerDp = 1f

    @Test fun renderedFirstLineIsStableAtNinetyPercent() {
        checkRenderedLine(fontScale = 0.9f)
    }

    @Test fun renderedFirstLineIsStableAtEightyFivePercent() {
        checkRenderedLine(fontScale = 0.85f)
    }

    private fun checkRenderedLine(fontScale: Float) {
        setContent(fontScale)
        val editor = compose.onNodeWithTag(PIXEL_COMMENT)
        editor.performClick()
        editor.performTextInputSelection(TextRange(TEST_TEXT.length))
        compose.waitForIdle()
        val originalInk = firstInkRow()
        val originalTop = editor.fetchSemanticsNode().boundsInRoot.top
        assertTrue("Test glyphs must be visible", originalInk >= 0)
        repeat(3) {
            editor.performTextInput("\n")
            compose.waitForIdle()
            assertEquals("Enter moved the rendered first line", originalInk, firstInkRow())
            assertEquals("Enter moved the field", originalTop, editor.fetchSemanticsNode().boundsInRoot.top, 0.01f)
            editor.performKeyInput { pressKey(Key.Backspace) }
            compose.waitForIdle()
            assertEquals("Backspace moved the rendered first line", originalInk, firstInkRow())
        }
    }

    private fun setContent(fontScale: Float) {
        compose.setContent {
            val density = LocalDensity.current
            pixelsPerDp = density.density
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme(colorScheme = darkColorScheme()) {
                    var value by remember { mutableStateOf(TextFieldValue(TEST_TEXT, TextRange(TEST_TEXT.length))) }
                    ComposerCommentField(
                        value = value,
                        limits = ComposerLimits(40 * 1024 * 1024, 4, 15_000),
                        enabled = true,
                        focusRequester = remember { FocusRequester() },
                        modifier = Modifier.testTag(PIXEL_COMMENT),
                        onChange = { value = it },
                    )
                }
            }
        }
    }

    private fun firstInkRow(): Int {
        val pixels = compose.onNodeWithTag(PIXEL_COMMENT).captureToImage().toPixelMap()
        val left = (17 * pixelsPerDp).toInt().coerceAtMost(pixels.width - 1)
        val right = (120 * pixelsPerDp).toInt().coerceAtMost(pixels.width)
        val top = (12 * pixelsPerDp).toInt().coerceAtMost(pixels.height - 1)
        val bottom = (50 * pixelsPerDp).toInt().coerceAtMost(pixels.height)
        for (y in top until bottom) {
            var inkPixels = 0
            for (x in left until right) {
                val color = pixels[x, y]
                val minimum = minOf(color.red, color.green, color.blue)
                val maximum = maxOf(color.red, color.green, color.blue)
                // Neutral bright glyphs only: exclude the purple border and blinking cursor.
                if (minimum > 0.45f && maximum - minimum < 0.18f) inkPixels++
            }
            if (inkPixels >= 3) return y
        }
        return -1
    }
}

private const val PIXEL_COMMENT = "composer-comment-pixels"
private const val TEST_TEXT = "ллдд"
