package com.solthariel.pawchan

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.settings.FilterLineInput
import com.solthariel.pawchan.settings.filterConditionPresentation
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ContentFilterInputSizeRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun filledConditionKeepsHeightWhenModeChanges() = checkModeChanges("ГОЛОВА ДАЙ ДЕНЕГ", 1f)

    @Test fun emptyConditionKeepsHeightWhenHintChanges() = checkModeChanges("", 1f)

    @Test fun largeTextKeepsHeightWhenHintChanges() = checkModeChanges("", 1.5f)

    @Test fun smallTextKeepsHeightWhenHintChanges() = checkModeChanges("", 0.9f)

    @Test fun newlinesKeepInputHeightAndFollowingContentPosition() {
        var text by mutableStateOf("строка")
        compose.setContent {
            MaterialTheme {
                Column(Modifier.width(240.dp)) {
                    FilterLineInput("Условие", text, { text = it })
                    Text("После поля", Modifier.testTag("after"))
                }
            }
        }
        val before = compose.onNodeWithTag("after").fetchSemanticsNode().boundsInRoot
        val input = compose.onNodeWithContentDescription("Условие")
        val initialHeight = input.fetchSemanticsNode().boundsInRoot.height
        val baseline = firstBaseline()
        listOf("строка\n", "строка\nдругая\n", "строка\nдругая\nтретья\nчетвёртая").forEach { value ->
            input.performTextReplacement(value)
            compose.runOnIdle { assertEquals(value, text) }
            assertEquals(baseline, firstBaseline(), 0.01f)
            assertEquals(initialHeight, input.fetchSemanticsNode().boundsInRoot.height, 1f)
            assertEquals(before, compose.onNodeWithTag("after").fetchSemanticsNode().boundsInRoot)
        }
    }

    private fun firstBaseline(): Float {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithContentDescription("Условие")
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single().firstBaseline
    }

    private fun checkModeChanges(
        text: String,
        fontScale: Float,
    ) {
        var mode by mutableStateOf(ContentFilterMode.EXACT_PHRASE)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Column(Modifier.width(240.dp).testTag("editor")) {
                        FilterLineInput("Условие", text, {}, presentation = filterConditionPresentation(mode.name))
                        Text("После поля", Modifier.testTag("after"))
                    }
                }
            }
        }
        val before = compose.onNodeWithTag("editor").fetchSemanticsNode().boundsInRoot
        val marker = compose.onNodeWithTag("after").fetchSemanticsNode().boundsInRoot
        repeat(3) {
            compose.runOnIdle { mode = ContentFilterMode.ALL_WORDS }
            assertEquals(before, compose.onNodeWithTag("editor").fetchSemanticsNode().boundsInRoot)
            assertEquals(marker, compose.onNodeWithTag("after").fetchSemanticsNode().boundsInRoot)
            compose.runOnIdle { mode = ContentFilterMode.EXACT_PHRASE }
            assertEquals(before, compose.onNodeWithTag("editor").fetchSemanticsNode().boundsInRoot)
            assertEquals(marker, compose.onNodeWithTag("after").fetchSemanticsNode().boundsInRoot)
        }
    }
}
