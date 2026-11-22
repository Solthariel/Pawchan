package com.solthariel.pawchan

import com.solthariel.pawchan.core.ui.DARK_POST_TEXT_COLORS
import com.solthariel.pawchan.core.ui.LIGHT_POST_TEXT_COLORS
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.thread.MONKEY_BLOCK_TAG
import com.solthariel.pawchan.thread.MONKEY_HEADING
import com.solthariel.pawchan.thread.REFERENCE_TAG
import com.solthariel.pawchan.thread.RichPostTextUi
import com.solthariel.pawchan.thread.SPOILER_TAG
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class MonkeyReplyPresentationTest {
    @Test fun onlyMarkedBodyGetsThePanelAndTogglingNeverRenumbersOriginalSpoilers() {
        val doc =
            PostDocument(
                listOf(
                    PostTextRun(">>2773736", link = PostLink.Reference(requireNotNull(PostId.parse(2773736)))),
                    PostTextRun("\n@monkey вопрос\n"),
                    PostTextRun("ответ\n", monkeyBlock = 1),
                    PostTextRun("хвост", setOf(PostTextStyle.SPOILER)),
                ),
            )
        val model = RichPostTextUi.create(doc)
        listOf(LIGHT_POST_TEXT_COLORS, DARK_POST_TEXT_COLORS).forEach { colors ->
            val shown = model.text(colors)
            val marked = shown.getStringAnnotations(MONKEY_BLOCK_TAG, 0, shown.length).single()
            assertEquals("$MONKEY_HEADING\nответ\n", shown.text.substring(marked.start, marked.end))
            val author = model.text(colors, hideMonkeyReplies = true)
            assertEquals(">>2773736\n@monkey вопрос\nхвост", author.text)
            assertTrue(author.getStringAnnotations(MONKEY_BLOCK_TAG, 0, author.length).isEmpty())
            assertEquals(1, author.getStringAnnotations(REFERENCE_TAG, 0, author.length).size)
            assertEquals("3", author.getStringAnnotations(SPOILER_TAG, 0, author.length).single().item)
            val revealed = model.reveal(setOf(3), colors, hideMonkeyReplies = true)
            assertTrue(revealed.getStringAnnotations(SPOILER_TAG, 0, revealed.length).isEmpty())
            assertSame(shown, model.text(colors))
        }
        assertFalse(doc.plainText.contains(MONKEY_HEADING))
    }

    @Test fun suppressingOnlyBotTextLeavesNoCaptionOrPlaceholder() {
        val model = RichPostTextUi.create(PostDocument(listOf(PostTextRun("ответ\n", monkeyBlock = 1))))
        assertTrue(model.text(LIGHT_POST_TEXT_COLORS, true).isEmpty())
        val ordinary = RichPostTextUi.create(PostDocument.plain("@monkey без ответа"))
        assertSame(ordinary.text(LIGHT_POST_TEXT_COLORS), ordinary.text(LIGHT_POST_TEXT_COLORS, true))
    }
}
