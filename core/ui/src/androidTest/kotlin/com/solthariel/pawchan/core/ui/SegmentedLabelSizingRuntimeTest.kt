package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs

class SegmentedLabelSizingRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun ordinaryLabelsHaveEqualVisibleButtonHeightsOfAtLeast48Dp() {
        val labels = listOf("Фраза", "Любое слово", "Все слова")
        showOptions(labels, 1f)
        labels.forEach(::assertFullLabel)
        val bounds = labels.map { compose.onNodeWithText(it).fetchSemanticsNode().boundsInRoot }
        bounds.forEach { assertTrue("Unequal button height: $bounds", abs(it.height - bounds.first().height) <= 1f) }
    }

    @Test fun singleShortLargeFontLabelExposesItsFullHeightToMaterialContent() {
        showOptions(listOf("Фраза"), 1.8f)
        assertFullLabel("Фраза")
    }

    @Test fun semanticParagraphWidthMatchesRenderedShortLabelInsteadOfParentWidth() {
        showOptions(listOf("Фраза"), 1.8f)
        val layouts = mutableListOf<TextLayoutResult>()
        val label = compose.onNodeWithText("Фраза", useUnmergedTree = true)
        label.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val bounds = label.fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            assertTrue("No text layout", layouts.isNotEmpty())
            val layout = layouts.first()
            assertTrue(
                "Test must leave room around the short label",
                layout.layoutInput.constraints.maxWidth > bounds.width,
            )
            assertEquals("Semantics must describe the drawn label", bounds.width, layout.size.width.toFloat(), 1f)
            assertTrue("Paragraph wider than drawn label: $layout", layout.multiParagraph.width <= layout.size.width)
            assertFalse("Actual label overflow: $layout", layout.hasVisualOverflow)
        }
    }

    @Test fun allLargeFontLabelsFitWithoutShrinkingOrClipping() {
        val labels = listOf("Фраза", "Любое слово", "Все слова")
        showOptions(labels, 1.8f)
        labels.forEach(::assertFullLabel)
    }

    @Test fun largeFontLabelCanGrowAboveTheMinimumTouchHeight() {
        showOptions(listOf("Любое слово"), 1.8f)
        assertFullLabel("Любое слово")
    }

    @Test fun multipleLinesRemainVisibleAndSelectionStillDispatchesOnce() {
        var calls = 0
        val label = "Любое\nслово\nили фраза"
        showOptions(listOf("Фраза", label), 2f) { calls++ }
        assertFullLabel("Фраза")
        assertFullLabel(label)
        compose.onNodeWithText(label).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(1, calls) }
    }

    private fun showOptions(
        labels: List<String>,
        fontScale: Float,
        onSelect: () -> Unit = {},
    ) {
        var selected by mutableIntStateOf(0)
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(280.dp)) {
                        UiChoiceSegments(labels, selected, true, wrapLabels = true) {
                            selected = it
                            onSelect()
                        }
                    }
                }
            }
        }
    }

    private fun assertFullLabel(label: String) {
        compose.onNodeWithText(label).assertHeightIsAtLeast(48.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(label, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val textBounds = compose.onNodeWithText(label, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val buttonBounds = compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot
        compose.runOnIdle {
            assertTrue("Text outside button: $textBounds, $buttonBounds", buttonBounds.contains(textBounds.topLeft))
            assertTrue("Text outside button: $textBounds, $buttonBounds", textBounds.bottom <= buttonBounds.bottom + 1f)
            assertTrue("Text outside button: $textBounds, $buttonBounds", textBounds.right <= buttonBounds.right + 1f)
            assertEquals("Label not centered: $label", buttonBounds.center.y, textBounds.center.y, 1f)
            assertTrue("No text layout: $label", layouts.isNotEmpty())
            assertFalse("Clipped label: $label, layout=${layouts.first()}", layouts.first().hasVisualOverflow)
        }
    }
}
