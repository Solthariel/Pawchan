package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ComposerFormattingInteractionRuntimeTest(
    private val enabled: Boolean,
    private val loading: Boolean,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun formattingAndClearRespectEnabledAndLoadingState() {
        val edits = mutableListOf<CommentMarkup>()
        var clears = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(320.dp)) {
                    ComposerFormattingToolbar(enabled, loading, { clears++ }, edits::add)
                }
            }
        }
        CommentMarkup.entries.forEach { markup ->
            val key = compose.onNodeWithText(markup.label)
            if (enabled && !loading) key.assertIsEnabled() else key.assertIsNotEnabled()
            key.performClick()
        }
        val clear = compose.onNodeWithContentDescription("Удалить всё")
        if (enabled) clear.assertIsEnabled() else clear.assertIsNotEnabled()
        clear.performClick()
        compose.runOnIdle {
            assertEquals(if (enabled && !loading) CommentMarkup.entries.toList() else emptyList<CommentMarkup>(), edits)
            assertEquals(if (enabled) 1 else 0, clears)
        }
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "enabled={0} loading={1}")
        fun cases(): List<Array<Any>> =
            listOf(true, false).flatMap { enabled ->
                listOf(true, false).map { loading -> arrayOf(enabled, loading) }
            }
    }
}
