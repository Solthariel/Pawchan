package com.solthariel.pawchan

import androidx.compose.ui.text.font.FontWeight
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
import com.solthariel.pawchan.thread.REFERENCE_TAG
import com.solthariel.pawchan.thread.ReplyReferenceContext
import com.solthariel.pawchan.thread.RichPostTextUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplyReferenceHighlightRuntimeTest {
    @Test fun onlyTheExactSourceReferenceIsEmphasizedWithoutChangingTextOrLinks() {
        val model = model()
        val emphasized = model.withReplyContext(ReplyReferenceContext(address(1), 605)).initialText
        assertEquals(model.initialText.text, emphasized.text)
        assertEquals(
            model.initialText.getStringAnnotations(REFERENCE_TAG, 0, model.initialText.length),
            emphasized.getStringAnnotations(REFERENCE_TAG, 0, emphasized.length),
        )
        val ranges = emphasized.spanStyles.filter { it.item.fontWeight == FontWeight.SemiBold }
        assertEquals(1, ranges.size)
        assertEquals(">>605", emphasized.text.substring(ranges.single().start, ranges.single().end))
        assertEquals(0, model.initialText.spanStyles.count { it.item.fontWeight == FontWeight.SemiBold })
    }

    @Test fun matchingNumberInAnotherThreadIsNotHighlighted() {
        val text = model().withReplyContext(ReplyReferenceContext(address(3), 605)).initialText
        assertFalse(text.spanStyles.any { it.item.fontWeight == FontWeight.SemiBold })
    }

    @Test fun revealingSpoilerPreservesContextHighlightWithoutRevealingItEarly() {
        val document = PostDocument(listOf(PostTextRun(">>605", setOf(PostTextStyle.SPOILER), reference(605, 1))))
        val model = RichPostTextUi.create(document).withReplyContext(ReplyReferenceContext(address(1), 605))
        assertFalse(model.initialText.spanStyles.any { it.item.fontWeight == FontWeight.SemiBold })
        assertTrue(model.reveal(setOf(0)).spanStyles.any { it.item.fontWeight == FontWeight.SemiBold })
    }

    private fun model(): RichPostTextUi =
        RichPostTextUi.create(
            PostDocument(
                listOf(
                    PostTextRun(">>591", link = reference(591, 1)),
                    PostTextRun(" текст "),
                    PostTextRun(">>605", link = reference(605, 1)),
                    PostTextRun(" "),
                    PostTextRun(">>605", link = reference(605, 2)),
                ),
            ),
        )

    private fun reference(
        id: Long,
        thread: Long,
    ): PostLink.Reference = PostLink.Reference(requireNotNull(PostId.parse(id)), address(thread))

    private fun address(thread: Long): ThreadAddress =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(thread)),
        )
}
