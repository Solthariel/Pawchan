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
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.settings.FilterOptionGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FilterOptionGroupRuntimeTest {
    @get:Rule val compose = createComposeRule()
    private val options = listOf("phrase" to "Фраза", "any" to "Любой вариант", "all" to "Все условия")

    @Test fun wholeButtonsSelectExactlyOneOptionAndRepeatedClickDoesNotClearIt() {
        var selected by mutableStateOf("phrase")
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 1f)) {
                MaterialTheme {
                    Box(Modifier.width(360.dp)) {
                        FilterOptionGroup("Как искать", options, selected) { selected = it }
                    }
                }
            }
        }
        compose.onNodeWithText("Фраза").assertIsSelected()
        compose.onNodeWithText("Любой вариант").performClick().assertIsSelected()
        compose.onNodeWithText("Фраза").assertIsNotSelected()
        compose.onNodeWithText("Все условия").assertIsNotSelected()
        compose.onNodeWithText("Любой вариант").performClick().assertIsSelected()
        options.forEach { (_, label) -> compose.onNodeWithText(label).assertHeightIsAtLeast(48.dp) }
        val bounds = options.map { (_, label) -> compose.onNodeWithText(label).fetchSemanticsNode().boundsInRoot }
        assertTrue(bounds.all { kotlin.math.abs(it.top - bounds.first().top) < 1f })
        bounds.forEach {
            assertEquals(bounds.first().width, it.width, 1f)
            assertEquals(bounds.first().height, it.height, 1f)
        }
    }

    @Test fun largeFontsWrapChoicesWithoutOverlappingOrShrinkingTouchTargets() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                MaterialTheme { Box(Modifier.width(240.dp)) { FilterOptionGroup("Как искать", options, "phrase", {}) } }
            }
        }
        val bounds =
            options.map { (_, label) ->
                compose
                    .onNodeWithText(label)
                    .assertHeightIsAtLeast(48.dp)
                    .fetchSemanticsNode()
                    .boundsInRoot
            }
        assertTrue(bounds.last().top > bounds.first().top)
        bounds.forEachIndexed { index, rect ->
            bounds.drop(index + 1).forEach { other ->
                assertTrue(
                    rect.right <= other.left || other.right <= rect.left ||
                        rect.bottom <= other.top || other.bottom <= rect.top,
                )
            }
        }
    }
}
