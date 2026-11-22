package com.solthariel.pawchan.core.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PostCardSurfaceRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun sharedCardKeepsStandardPaddingAndSpacing() {
        var expectedPadding = 0f
        var expectedGap = 0f
        compose.setContent {
            val density = LocalDensity.current
            expectedPadding = with(density) { 8.dp.toPx() }
            expectedGap = with(density) { 6.dp.toPx() }
            MaterialTheme {
                PostCardSurface {
                    Text("Заголовок")
                    Text("Текст")
                }
            }
        }
        val first = compose.onNodeWithText("Заголовок").fetchSemanticsNode().boundsInRoot
        val second = compose.onNodeWithText("Текст").fetchSemanticsNode().boundsInRoot
        assertEquals(expectedPadding, first.left, 1f)
        assertEquals(expectedPadding, first.top, 1f)
        assertEquals(expectedGap, second.top - first.bottom, 1f)
    }
}
