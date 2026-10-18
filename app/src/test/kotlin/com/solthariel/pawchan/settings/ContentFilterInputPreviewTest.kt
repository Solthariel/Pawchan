package com.solthariel.pawchan.settings

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ContentFilterInputPreviewTest {
    @Test
    fun matchedTextKeepsItsOriginalCharactersAndStylesInsideTheInput() {
        val input = AnnotatedString("Ёжик 🦔")
        val preview =
            buildAnnotatedString {
                append(input)
                addStyle(SpanStyle(background = Color.Yellow), 0, input.length)
            }
        val result = filterInputPreview(input, preview)
        assertEquals(input.text, result.text.text)
        assertEquals(preview.spanStyles, result.text.spanStyles)
        (0..input.length).forEach { offset ->
            assertEquals(offset, result.offsetMapping.originalToTransformed(offset))
            assertEquals(offset, result.offsetMapping.transformedToOriginal(offset))
        }
    }

    @Test
    fun staleHighlightNeverReplacesMoreRecentlyTypedText() {
        val input = AnnotatedString("новый текст")
        val stale =
            buildAnnotatedString {
                append("старый текст")
                addStyle(SpanStyle(background = Color.Yellow), 0, length)
            }
        val result = filterInputPreview(input, stale)
        assertEquals(input, result.text)
        assertTrue(result.text.spanStyles.isEmpty())
    }

    @Test
    fun absentProbeLeavesRawInputAndSelectionOffsetsUntouched() {
        val input = AnnotatedString("проверяемый\nтекст")
        val result = filterInputPreview(input, null)
        assertEquals(input, result.text)
        assertEquals(input.length, result.offsetMapping.originalToTransformed(input.length))
        assertEquals(input.length, result.offsetMapping.transformedToOriginal(input.length))
    }
}
