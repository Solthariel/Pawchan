package com.solthariel.pawchan.core.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MaterialControlsRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun segmentSelectionDispatchesOnceAndPreservesTheSelectedValue() {
        var selected by mutableIntStateOf(0)
        var calls = 0
        compose.setContent {
            MaterialTheme {
                UiChoiceSegments(listOf("Фраза", "Любое слово", "Все слова"), selected, true) {
                    selected = it
                    calls++
                }
            }
        }
        compose.onNodeWithText("Фраза").assertIsSelected()
        compose.onNodeWithText("Все слова").performClick().assertIsSelected()
        compose.onNodeWithText("Фраза").assertIsNotSelected()
        compose.runOnIdle {
            assertEquals(2, selected)
            assertEquals(1, calls)
        }
    }

    @Test fun disabledSegmentsNeverDispatchEvenForTouchInput() {
        var calls = 0
        compose.setContent { MaterialTheme { UiChoiceSegments(listOf("JPEG", "PNG"), 0, false) { calls++ } } }
        compose.onNodeWithText("PNG").assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithText("JPEG").assertIsSelected()
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun sliderAccessibilityChangesTheValueAndCommitsOnce() {
        var value by mutableStateOf(0f)
        var commits = 0
        compose.setContent {
            MaterialTheme { CompactSlider(value, { value = it }, 0f..1f, onValueChangeFinished = { commits++ }) }
        }
        compose
            .onNode(
                androidx.compose.ui.test.SemanticsMatcher
                    .keyIsDefined(SemanticsActions.SetProgress),
            ).performSemanticsAction(SemanticsActions.SetProgress) { action -> action(0.75f) }
        compose.runOnIdle {
            assertEquals(0.75f, value, 0.001f)
            assertEquals(1, commits)
        }
    }

    @Test fun qualityLabelsFitANarrowRowWithoutClipping() {
        compose.setContent { MaterialTheme { Box(Modifier.width(280.dp)) { ImageQualityControl(100, true) {} } } }
        listOf("75%", "85%", "95%", "100%").forEach { label ->
            val layouts = mutableListOf<TextLayoutResult>()
            compose
                .onNodeWithText(label, useUnmergedTree = true)
                .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            compose.runOnIdle {
                assertTrue(layouts.isNotEmpty())
                assertFalse(layouts.first().hasVisualOverflow)
            }
        }
    }
}
