package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DvachMonkeyPostTest {
    @Test fun suppliedPostSeparatesQuestionFromAnswerThroughTheRealApiDecoder() {
        val html = requireNotNull(javaClass.getResource("/fixtures/dvach-monkey-2773740.html")).readText().trim()
        val address =
            ThreadAddress(
                BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("mobi"))),
                requireNotNull(ThreadId.parse(2730389L)),
            )
        val payload =
            """{"threads":[{"posts":[{"num":2730389,"parent":0,"comment":"ОП"},""" +
                """{"num":2773740,"parent":2730389,"comment":${JsonPrimitive(html)}}]}]}"""
        val decoder = ThreadPayloadDecoder(Json { ignoreUnknownKeys = true })
        val post = requireNotNull(decoder.decode(address, payload)).posts.last()
        assertEquals(2773740L, post.address.post.value)
        val runs = post.content.document.runs
        val human = runs.filter { it.monkeyBlock == null }
        assertEquals(2773736L, (human.first().link as PostLink.Reference).post.value)
        assertEquals(
            ">>2773736\n@monkey разложи его набор слов по понятному таймлайну",
            human.joinToString("") { it.text }.trimEnd(),
        )
        val answer = runs.filter { it.monkeyBlock == 1 }.joinToString("") { it.text }
        assertTrue(answer.startsWith("Его текст это не разбор, а три точки.\n\nРаньше"))
        assertTrue(answer.trimEnd().endsWith("откатились» нет."))
    }

    @Test fun mentionSimilarClassAndOtherAdaptersNeverClassifyHumanText() {
        val ordinary = "@monkey <div class='neurosloppy'>вопрос</div> &lt;div class=neuroslop&gt;"
        assertFalse(PostDocumentParser.parseDvach(ordinary).hasMonkeyReply)
        val marked = "<div class='neuroslop'>body</div>"
        assertFalse(PostDocumentParser.parse(marked).hasMonkeyReply)
        assertFalse(parseFourChanDocument(marked).hasMonkeyReply)
        assertFalse(PostDocumentParser.parseDvach("a<div class='neuroslop'> <br> </div>b").hasMonkeyReply)
    }

    @Test fun nestedMarkupKeepsItsStructureAndAdjacentAnswerBoxesStaySeparate() {
        val html =
            "до<div class='x neuroslop y'><b>раз</b><br><span class='spoiler'>два</span>" +
                "<a href='/b/res/8.html#9'>&gt;&gt;9</a></div><div class='neuroslop'>ещё</div>после"
        val runs = PostDocumentParser.parseDvach(html).runs
        assertEquals(setOf(1, 2), runs.mapNotNull { it.monkeyBlock }.toSet())
        assertTrue(runs.any { it.monkeyBlock == 1 && PostTextStyle.BOLD in it.styles })
        assertTrue(runs.any { it.monkeyBlock == 1 && PostTextStyle.SPOILER in it.styles })
        assertTrue(runs.any { it.monkeyBlock == 1 && it.link is PostLink.Reference })
        assertEquals("до\nпосле", runs.filter { it.monkeyBlock == null }.joinToString("") { it.text })
    }
}
