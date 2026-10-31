package com.solthariel.pawchan

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.solthariel.pawchan.core.ui.CompactSwitch
import com.solthariel.pawchan.settings.SettingsItem
import com.solthariel.pawchan.settings.SettingsItemRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CompactSettingsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun shortCaptionsUseOneLineAtNormalFontWithoutShrinkingIt() {
        show(1f)
        listOf("При запуске", "Для каждого треда").forEach { caption ->
            val layouts = layouts(caption)
            val result = layouts.single()
            val details = layoutDetails(caption, result)
            assertEquals(details, 1, result.lineCount)
            assertCaptionVisible(caption, result)
            assertEquals(details, 14.sp, result.layoutInput.style.fontSize)
            assertEquals(details, 20.sp, result.layoutInput.style.lineHeight)
        }
    }

    @Test fun largerSystemFontMayWrapAndTheWholeRowStillToggles() {
        show(2f)
        listOf("При запуске", "Для каждого треда").forEach { caption ->
            val result = layouts(caption).single()
            assertCaptionVisible(caption, result)
        }
        compose
            .onNodeWithText("Восстанавливать последний экран")
            .assertHeightIsAtLeast(48.dp)
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }

    @Test fun compactSwitchVisualDoesNotReserveTheLargeMaterialSwitchWidth() {
        var pixelsPerDp = 0f
        compose.setContent {
            MaterialTheme {
                pixelsPerDp = LocalDensity.current.density
                CompactSwitch(true, Modifier.testTag("compact-switch-visual"))
            }
        }
        val bounds = compose.onNodeWithTag("compact-switch-visual").fetchSemanticsNode().boundsInRoot
        assertEquals(48f * pixelsPerDp, bounds.width, 1f)
        assertEquals(32f * pixelsPerDp, bounds.height, 1f)
    }

    private fun assertCaptionVisible(
        caption: String,
        result: TextLayoutResult,
    ) {
        val details = layoutDetails(caption, result)
        val bounds = compose.onNodeWithText(caption, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(details, caption, result.layoutInput.text.text)
        assertTrue(details, result.lineCount > 0)
        assertEquals(details, caption.length, result.getLineEnd(result.lineCount - 1, visibleEnd = true))
        // ListItem may reuse a wider MultiParagraph when it remeasures a short caption.
        // Check actual lines and clipped node bounds, not size.width < multiParagraph.width.
        repeat(result.lineCount) { line ->
            val lineDetails =
                "$details, line=$line, left=${result.getLineLeft(line)}, right=${result.getLineRight(line)}, " +
                    "top=${result.getLineTop(line)}, bottom=${result.getLineBottom(line)}, bounds=$bounds"
            assertFalse(lineDetails, result.isLineEllipsized(line))
            assertTrue(lineDetails, result.getLineLeft(line) >= -PIXEL_ROUNDING_TOLERANCE)
            assertTrue(lineDetails, result.getLineRight(line) <= result.size.width + PIXEL_ROUNDING_TOLERANCE)
            assertTrue(lineDetails, result.getLineTop(line) >= -PIXEL_ROUNDING_TOLERANCE)
            assertTrue(lineDetails, result.getLineBottom(line) <= result.size.height + PIXEL_ROUNDING_TOLERANCE)
        }
        assertTrue(details, bounds.width + PIXEL_ROUNDING_TOLERANCE >= result.size.width)
        assertTrue(details, bounds.height + PIXEL_ROUNDING_TOLERANCE >= result.size.height)
    }

    private fun layoutDetails(
        text: String,
        result: TextLayoutResult,
    ): String {
        val details =
            "$text: lines=${result.lineCount}, size=${result.size}, " +
                "paragraphWidth=${result.multiParagraph.width}, " +
                "constraints=${result.layoutInput.constraints}, " +
                "widthOverflow=${result.didOverflowWidth}, heightOverflow=${result.didOverflowHeight}"
        Log.i("PawchanUiTest", details)
        return details
    }

    private fun layouts(text: String): List<TextLayoutResult> {
        val results = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(results) }
        return results
    }

    private fun show(fontScale: Float) {
        var checked by mutableStateOf(true)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(320.dp)) {
                        Column {
                            SettingsItemRow(
                                SettingsItem.Toggle(
                                    "Восстанавливать последний экран",
                                    "При запуске",
                                    checked,
                                ) { checked = it },
                            )
                            SettingsItemRow(
                                SettingsItem.Toggle("Запоминать позицию чтения", "Для каждого треда", true) {},
                            )
                        }
                    }
                }
            }
        }
    }
}

private const val PIXEL_ROUNDING_TOLERANCE = 1f
