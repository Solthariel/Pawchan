package com.solthariel.pawchan

import androidx.compose.ui.text.AnnotatedString
import com.solthariel.pawchan.thread.EXTERNAL_TAG
import com.solthariel.pawchan.thread.REFERENCE_TAG
import com.solthariel.pawchan.thread.SPOILER_TAG
import com.solthariel.pawchan.thread.postLinkAnnotationAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostLinkPressAnnotationTest {
    @Test
    fun referenceAndExternalRangesAreHalfOpen() {
        for (tag in listOf(REFERENCE_TAG, EXTERNAL_TAG)) {
            val text = annotated(tag)
            assertNull(postLinkAnnotationAt(text, 0))
            assertEquals(tag, postLinkAnnotationAt(text, 1)?.tag)
            assertEquals(tag, postLinkAnnotationAt(text, 3)?.tag)
            assertNull(postLinkAnnotationAt(text, 4))
        }
    }

    @Test
    fun hiddenSpoilerTakesPriorityOverOverlappingLink() {
        val text =
            AnnotatedString
                .Builder("abcdef")
                .apply {
                    addStringAnnotation(SPOILER_TAG, "0", 1, 4)
                    addStringAnnotation(EXTERNAL_TAG, "https://2ch.hk", 1, 4)
                }.toAnnotatedString()
        assertNull(postLinkAnnotationAt(text, 2))
    }

    @Test
    fun hiddenSpoilerWinsEvenWhenLinkAnnotationWasInsertedFirst() {
        val text =
            AnnotatedString
                .Builder("abcdef")
                .apply {
                    addStringAnnotation(EXTERNAL_TAG, "https://2ch.hk", 1, 4)
                    addStringAnnotation(SPOILER_TAG, "0", 1, 4)
                }.toAnnotatedString()
        assertNull(postLinkAnnotationAt(text, 2))
    }

    @Test
    fun plainAndSpoilerTextHaveNoPressedLink() {
        assertNull(postLinkAnnotationAt(AnnotatedString("abcdef"), 2))
        assertNull(postLinkAnnotationAt(annotated(SPOILER_TAG), 2))
    }

    private fun annotated(tag: String): AnnotatedString =
        AnnotatedString
            .Builder("abcdef")
            .apply {
                addStringAnnotation(tag, "value", 1, 4)
            }.toAnnotatedString()
}
