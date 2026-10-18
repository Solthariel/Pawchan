package com.solthariel.pawchan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.thread.EXTERNAL_TAG
import com.solthariel.pawchan.thread.REFERENCE_TAG
import com.solthariel.pawchan.thread.RichTextTapActions
import com.solthariel.pawchan.thread.SPOILER_TAG
import com.solthariel.pawchan.thread.richPostGestures
import com.solthariel.pawchan.thread.richPostLinkAt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class PostLinkGestureTargetTest {
    @Test
    fun linkHitTestingUsesHalfOpenRangeAndKeepsSpoilersHidden() {
        val text = annotated(">>456 x", REFERENCE_TAG, "456", 5)
        assertEquals(456L, (richPostLinkAt(text, 2) as? PostLink.Reference)?.post?.value)
        assertNull(richPostLinkAt(text, 5))
        val hidden = annotated("hidden", SPOILER_TAG, "0", 6)
        assertNull(richPostLinkAt(hidden, 2))
    }

    @Test
    fun linkLongPressDoesNotOpenPreviewBrowserOrPostMenu() {
        val text = annotated("link", EXTERNAL_TAG, "https://example.test/", 4)
        var links = 0
        var posts = 0
        var opened = false
        val actions =
            RichTextTapActions(text, { opened = true }, { opened = true }, { opened = true }, {
                links++
                true
            })
        val gestures = richPostGestures(actions, {}, { 1 }, { _: TextRange -> }, { posts++ })
        gestures.onLongPress(Offset.Zero)
        assertEquals(1, links)
        assertEquals(0, posts)
        assertFalse(opened)
    }

    @Test
    fun nonLinkLongPressPreservesExistingPostMenu() {
        var posts = 0
        val actions = RichTextTapActions(AnnotatedString("plain"), {}, null, {}, { error("Not a link") })
        richPostGestures(actions, {}, { 2 }, {}, { posts++ }).onLongPress(Offset.Zero)
        assertEquals(1, posts)
    }

    private fun annotated(
        text: String,
        tag: String,
        item: String,
        end: Int,
    ): AnnotatedString {
        val builder = AnnotatedString.Builder(text)
        builder.addStringAnnotation(tag, item, 0, end)
        return builder.toAnnotatedString()
    }
}
