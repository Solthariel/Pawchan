package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ComposerFormattingGeometryRuntimeTest(
    private val width: Int,
    private val fontScale: Float,
) {
    @get:Rule val compose = createComposeRule()

    @Test fun toolbarMatchesFieldEdgesAndAllKeysRemainReachable() {
        compose.setContent {
            DeviceConfigurationOverride(DeviceConfigurationOverride.ForcedSize(DpSize(width.dp, 600.dp))) {
                CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale)) {
                    MaterialTheme {
                        Column(Modifier.width(width.dp).padding(horizontal = 16.dp)) {
                            OutlinedTextField("", {}, Modifier.fillMaxWidth().testTag("field"))
                            ComposerFormattingToolbar(true, false, {}, {})
                        }
                    }
                }
            }
        }
        val field = compose.onNodeWithTag("field").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("composer-formatting-viewport").fetchSemanticsNode().boundsInRoot
        assertEquals(field.left, viewport.left, 1f)
        assertEquals(field.right, viewport.right, 1f)
        if (fontScale == 1f) {
            CommentMarkup.entries.forEach { markup ->
                compose
                    .onNodeWithTag("composer-markup-glyph-${markup.name}", useUnmergedTree = true)
                    .assertIsDisplayed()
            }
            val clear = compose.onNodeWithTag("composer-clear-glyph", useUnmergedTree = true)
            clear.assertIsDisplayed()
            assertEquals(field.right, clear.fetchSemanticsNode().boundsInRoot.right, 1f)
        }
        val bold = compose.onNodeWithTag("composer-markup-glyph-BOLD", useUnmergedTree = true)
        bold.assertIsDisplayed()
        val first = bold.fetchSemanticsNode().boundsInRoot
        assertEquals(field.left, first.left, 1f)
        CommentMarkup.entries.forEach { markup ->
            val key = compose.onNodeWithTag("composer-markup-glyph-${markup.name}", useUnmergedTree = true)
            key.performScrollTo().assertIsDisplayed()
            assertEquals(
                first.center.y,
                key
                    .fetchSemanticsNode()
                    .boundsInRoot.center.y,
                1f,
            )
        }
        val clear = compose.onNodeWithTag("composer-clear-glyph", useUnmergedTree = true)
        clear.performScrollTo().assertIsDisplayed()
        assertEquals(field.right, clear.fetchSemanticsNode().boundsInRoot.right, 1f)
        assertEquals(
            first.center.y,
            clear
                .fetchSemanticsNode()
                .boundsInRoot.center.y,
            1f,
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}dp / font {1}")
        fun cases(): List<Array<Any>> =
            listOf(320, 393, 600).flatMap { width ->
                listOf(1f, 2f).map { scale -> arrayOf(width, scale) }
            }
    }
}
