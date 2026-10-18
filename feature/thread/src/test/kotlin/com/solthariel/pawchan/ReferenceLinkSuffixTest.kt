package com.solthariel.pawchan

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.thread.LINK_SUFFIX_TAG
import com.solthariel.pawchan.thread.REFERENCE_TAG
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.linkSuffixLabel
import com.solthariel.pawchan.thread.withVisualLinkSuffixes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReferenceLinkSuffixTest {
    private val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("test")))
    private val root = thread(252013)
    private val other = thread(300000)

    @Test fun opSuffixKeepsOffsetsCopyTextAndHidesOnlyMarkerWithoutUnderline() {
        val original = RichPostTextUi.create(PostDocument(listOf(run(252013, root, " (OP)")))).initialText
        val visual = original.withVisualLinkSuffixes()
        assertEquals(">>252013 (OP)", visual.text)
        assertEquals(original.length, visual.length)
        assertEquals(
            original.getStringAnnotations(REFERENCE_TAG, 0, original.length),
            visual.getStringAnnotations(REFERENCE_TAG, 0, visual.length),
        )
        val marker = visual.getStringAnnotations(LINK_SUFFIX_TAG, 0, visual.length).single()
        assertEquals("(OP)", visual.text.substring(marker.start, marker.end))
        assertEquals("OP", linkSuffixLabel(marker.item, root))
        val styles = visual.spanStyles.filter { it.start <= marker.start - 1 && it.end >= marker.end }
        assertTrue(styles.any { it.item.textDecoration == TextDecoration.None })
        assertTrue(visual.spanStyles.any { it.start == marker.start && it.item.color == Color.Transparent })
    }

    @Test fun crossThreadArrowBecomesDifferentThreadSuffix() {
        val text = RichPostTextUi.create(PostDocument(listOf(run(300001, other, " →")))).initialText
        val marker = text.getStringAnnotations(LINK_SUFFIX_TAG, 0, text.length).single()
        assertEquals("→", text.text.substring(marker.start, marker.end))
        assertEquals("DT", linkSuffixLabel(marker.item, root))
    }

    @Test fun differentThreadWinsOverOriginalPoster() {
        val text = RichPostTextUi.create(PostDocument(listOf(run(300000, other, " (OP)")))).initialText
        val marker = text.getStringAnnotations(LINK_SUFFIX_TAG, 0, text.length).single()
        assertEquals("DT", linkSuffixLabel(marker.item, root))
        assertEquals("OP", linkSuffixLabel(marker.item, other))
        assertEquals("OP", linkSuffixLabel(marker.item, null))
    }

    @Test fun doesNotReplaceLiteralOrWrongPostMarkers() {
        val document = PostDocument(listOf(PostTextRun("literal (OP) →"), run(252473, root, " (OP)")))
        val text = RichPostTextUi.create(document).initialText
        assertTrue(text.getStringAnnotations(LINK_SUFFIX_TAG, 0, text.length).isEmpty())
        assertEquals(document.plainText, text.withVisualLinkSuffixes().text)
    }

    @Test fun hiddenSpoilerGetsNoVisibleSuffixUntilRevealed() {
        val spoiler = run(252013, root, " (OP)").copy(styles = setOf(PostTextStyle.SPOILER))
        val model = RichPostTextUi.create(PostDocument(listOf(spoiler)))
        assertTrue(model.initialText.getStringAnnotations(LINK_SUFFIX_TAG, 0, model.initialText.length).isEmpty())
        val revealed = model.reveal(setOf(0))
        assertEquals(1, revealed.getStringAnnotations(LINK_SUFFIX_TAG, 0, revealed.length).size)
    }

    @Test fun unknownReferenceContextDoesNotGuessSuffix() {
        val unknown = PostTextRun(">>252013 (OP)", link = PostLink.Reference(requireNotNull(PostId.parse(252013))))
        val text = RichPostTextUi.create(PostDocument(listOf(unknown))).initialText
        assertTrue(text.getStringAnnotations(LINK_SUFFIX_TAG, 0, text.length).isEmpty())
    }

    private fun thread(id: Long) = ThreadAddress(board, requireNotNull(ThreadId.parse(id)))

    private fun run(
        id: Long,
        thread: ThreadAddress,
        suffix: String,
    ): PostTextRun = PostTextRun(">>$id$suffix", link = PostLink.Reference(requireNotNull(PostId.parse(id)), thread))
}
