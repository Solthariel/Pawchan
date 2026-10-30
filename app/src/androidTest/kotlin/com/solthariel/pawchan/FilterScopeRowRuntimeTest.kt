package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.settings.FilterOptionGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FilterScopeRowRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val options = listOf("all" to "Все посты", "op" to "ОП-посты", "reply" to "Комментарии")

    @Test fun scopeHasEqualHorizontalCellsAtNormalFont() = verifyScope(320.dp, 1f)

    @Test fun scopeWrapsWithoutEllipsisAtLargeFontAndNarrowWidth() = verifyScope(240.dp, 2f)

    private fun verifyScope(
        width: Dp,
        fontScale: Float,
    ) {
        var selected by mutableStateOf("all")
        var sharedBorderPx = 0f
        compose.setContent {
            val density = LocalDensity.current
            sharedBorderPx = with(density) { 1.dp.toPx() }
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                MaterialTheme {
                    Box(Modifier.width(width)) {
                        FilterOptionGroup("Где скрывать", options, selected) { selected = it }
                    }
                }
            }
        }
        val bounds =
            options.map { (_, label) ->
                val node = compose.onNodeWithText(label).assertHeightIsAtLeast(48.dp)
                val layouts = mutableListOf<TextLayoutResult>()
                compose
                    .onNodeWithText(label, useUnmergedTree = true)
                    .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
                assertFalse(layouts.single().hasVisualOverflow)
                node.fetchSemanticsNode().boundsInRoot
            }
        bounds.forEach {
            assertEquals(bounds.first().top, it.top, 1f)
            assertEquals(bounds.first().width, it.width, 1f)
            assertEquals(bounds.first().height, it.height, 1f)
        }
        bounds.zipWithNext().forEach { (left, right) -> assertTrue(left.right <= right.left + sharedBorderPx + 1f) }
        compose.onNodeWithText("Комментарии").performClick().assertIsSelected()
        compose.onNodeWithText("Все посты").assertIsNotSelected()
        compose.runOnIdle { assertEquals("reply", selected) }
    }
}
