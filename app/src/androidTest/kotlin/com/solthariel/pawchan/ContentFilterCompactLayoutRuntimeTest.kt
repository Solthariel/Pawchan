package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.navigation.defaultBoardAddress
import com.solthariel.pawchan.settings.ContentFilterDraft
import com.solthariel.pawchan.settings.FilterEditorBody
import com.solthariel.pawchan.settings.FilterEditorFields
import com.solthariel.pawchan.settings.FilterLineInput
import com.solthariel.pawchan.settings.filterConditionPresentation
import com.solthariel.pawchan.settings.newContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ContentFilterCompactLayoutRuntimeTest {
    @get:Rule val compose = createComposeRule()

    @Test fun emptyAndFilledConditionsHaveOneLineInEitherSearchMode() {
        var text by mutableStateOf("")
        var mode by mutableStateOf(ContentFilterMode.PHRASE.name)
        var scale = 1f
        compose.setContent {
            val density = LocalDensity.current.density
            scale = density
            CompositionLocalProvider(LocalDensity provides Density(density, 1f)) {
                MaterialTheme {
                    Box(Modifier.width(280.dp)) {
                        FilterLineInput(
                            "Условие",
                            text,
                            { text = it },
                            presentation = filterConditionPresentation(mode),
                        )
                    }
                }
            }
        }
        val field = compose.onNodeWithContentDescription("Условие")
        assertTrue(field.fetchSemanticsNode().boundsInRoot.height / scale in 56f..72f)
        compose.runOnIdle { mode = ContentFilterMode.ALL_TERMS.name }
        assertTrue(field.fetchSemanticsNode().boundsInRoot.height / scale in 56f..72f)
        field.performTextReplacement("короткая фраза")
        assertTrue(field.fetchSemanticsNode().boundsInRoot.height / scale in 56f..72f)
        compose.runOnIdle { mode = ContentFilterMode.PHRASE.name }
        assertTrue(field.fetchSemanticsNode().boundsInRoot.height / scale in 56f..72f)
    }

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

    @Test fun enabledCheckboxSharesTheContentInsetAndKeepsTheWholeRowTouchable() {
        val draft = ContentFilterDraft(newContentFilterRule(defaultBoardAddress(), "кот"))
        compose.setContent {
            MaterialTheme {
                Box(Modifier.width(360.dp).height(700.dp)) {
                    FilterEditorBody(draft, {}, null, { TextButton(onClick = {}) { Text("Сохранить") } }) {
                        FilterEditorFields(draft)
                    }
                }
            }
        }
        val opLabel = compose.onNodeWithText("Учитывать регистр", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val enabled = compose.onNodeWithText("Включено", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        assertEquals(opLabel.left, enabled.left, 1f)
        compose.onNodeWithTag("filter-editor-enabled").assertHeightIsAtLeast(48.dp)
    }
}
