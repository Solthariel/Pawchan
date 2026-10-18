package com.solthariel.pawchan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.media.MediaCaption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MediaCaptionUiTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun narrowMmaCaptionHasOneLineAndRightAlignedDimensions() {
        compose.setContent {
            Box(Modifier.width(80.dp)) {
                MediaCaption("JPEG 183 КБ", "1440×1440", Modifier.testTag("caption"))
            }
        }
        assertSingleLineWithoutOverflow("JPEG 183 КБ")
        val layout = assertSingleLineWithoutOverflow("1440×1440")
        val left = compose.onNodeWithText("JPEG 183 КБ").fetchSemanticsNode().boundsInRoot
        val right = compose.onNodeWithText("1440×1440").fetchSemanticsNode().boundsInRoot
        val caption = compose.onNodeWithTag("caption").fetchSemanticsNode().boundsInRoot
        assertTrue(left.right < right.left)
        assertEquals(layout.size.width.toFloat(), layout.getLineRight(0), 1f)
        val density = compose.density.density
        assertEquals(caption.right - 3f * density, right.right, 1f)
    }

    @Test
    fun largeDimensionsAndSizeDoNotOverlap() {
        compose.setContent {
            Box(Modifier.width(80.dp)) {
                MediaCaption("WEBM 12.5 МБ", "10000×10000")
            }
        }
        assertSingleLineWithoutOverflow("WEBM 12.5 МБ")
        assertSingleLineWithoutOverflow("10000×10000")
    }

    @Test
    fun captionWithoutDimensionsStillShowsTypeAndSize() {
        compose.setContent {
            Box(Modifier.width(80.dp)) { MediaCaption("PNG 198 КБ", null) }
        }
        assertSingleLineWithoutOverflow("PNG 198 КБ")
    }

    private fun assertSingleLineWithoutOverflow(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        val layout = layouts.single()
        assertEquals(1, layout.lineCount)
        assertFalse(layout.hasVisualOverflow)
        return layout
    }
}
