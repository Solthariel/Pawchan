package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.settings.FilterLineInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentFilterCompactLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun actualLineBreaksArePreservedAndOnlyRealTextExpandsTheField() {
        var text by mutableStateOf("кот")
        compose.setContent {
            val density = LocalDensity.current.density
            CompositionLocalProvider(LocalDensity provides Density(density, 1f)) {
                MaterialTheme { Box(Modifier.width(280.dp)) { FilterLineInput("Условие", text, { text = it }) } }
            }
        }
        val field = compose.onNodeWithContentDescription("Условие")
        val initial = field.fetchSemanticsNode().boundsInRoot.height
        field.performTextReplacement("кот\nсобака")
        compose.runOnIdle { assertEquals("кот\nсобака", text) }
        assertTrue(field.fetchSemanticsNode().boundsInRoot.height > initial)
        field.performTextReplacement("кот")
        assertEquals(initial, field.fetchSemanticsNode().boundsInRoot.height, 1f)
    }
}
