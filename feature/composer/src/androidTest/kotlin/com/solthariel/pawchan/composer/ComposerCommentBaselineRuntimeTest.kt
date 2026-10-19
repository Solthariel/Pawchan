package com.solthariel.pawchan.composer

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Density
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ComposerCommentBaselineRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun firstBaselineDoesNotMoveWhenAddingAndRemovingNewlines() {
        checkFirstBaseline(fontScale = 1f, firstLine = "ллдд")
    }

    @Test fun firstBaselineIsStableWithLargeText() {
        checkFirstBaseline(fontScale = 1.5f, firstLine = "Йёлд")
    }

    @Test fun firstBaselineIsStableWithEmoji() {
        checkFirstBaseline(fontScale = 1f, firstLine = "Привет 🙂")
    }

    private fun checkFirstBaseline(
        fontScale: Float,
        firstLine: String,
    ) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    var value by remember { mutableStateOf(TextFieldValue(firstLine)) }
                    ComposerCommentField(
                        value = value,
                        limits = ComposerLimits(40 * 1024 * 1024, 4, 15_000),
                        enabled = true,
                        focusRequester = remember { FocusRequester() },
                        modifier = Modifier.testTag(COMMENT),
                        onChange = { value = it },
                    )
                }
            }
        }
        compose.waitForIdle()
        val original = layout()
        val firstBaseline = original.firstBaseline
        val fieldTop =
            compose
                .onNodeWithTag(COMMENT)
                .fetchSemanticsNode()
                .boundsInRoot.top
        val changes =
            listOf(
                "$firstLine\n",
                "$firstLine\nВторая строка",
                "$firstLine\nВторая строка\n",
                "$firstLine\nВторая строка\nТретья строка",
                "$firstLine\nВторая строка",
                "$firstLine\n",
                firstLine,
            )
        changes.forEach { text ->
            compose.onNodeWithTag(COMMENT).performTextReplacement(text)
            compose.waitForIdle()
            val current = layout()
            assertEquals("The editor must preserve entered text", text, current.layoutInput.text.text)
            assertEquals("First baseline moved for $text", firstBaseline, current.firstBaseline, 0.01f)
            assertEquals(
                "Field moved for $text",
                fieldTop,
                compose
                    .onNodeWithTag(COMMENT)
                    .fetchSemanticsNode()
                    .boundsInRoot.top,
                0.01f,
            )
            assertTrue("First line must have a positive height", current.getLineBottom(0) > 0f)
        }
    }

    private fun layout(): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithTag(COMMENT).performSemanticsAction(SemanticsActions.GetTextLayoutResult) {
            assertTrue("Text layout must be available", it(layouts))
        }
        return layouts.single()
    }
}

private const val COMMENT = "composer-comment-baseline"
