package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterEngineTest {
    private val board = BoardAddress(checkNotNull(SiteId.parse("2ch")), checkNotNull(BoardId.parse("b")))
    private val ordered =
        ContentFilterRule(
            "rule",
            "2ch",
            setOf("b"),
            targets = ContentFilterTargets(comments = true),
            condition = ContentFilterConditionSpec(mode = ContentFilterMode.ORDERED, values = listOf("Храм", "ГОЛОВЫ")),
        )

    @Test fun orderedFragmentsStayInOneField() {
        val engine = ContentFilterEngine(listOf(ordered))
        assertNotNull(engine.thread(board, "ХРАМ\n чего-то ГОЛОВЫ", ""))
        assertNull(engine.thread(board, "Храм", "ГОЛОВЫ"))
        assertNull(engine.comment(board, "ГОЛОВЫ потом Храм"))
    }

    @Test fun literalPhrasesNormalizeWhitespaceAndYo() {
        val rule =
            ordered.copy(
                condition =
                    ordered.condition.copy(
                        mode = ContentFilterMode.ANY_PHRASE,
                        values = listOf("ещё /pure/", "a.b"),
                    ),
            )
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.comment(board, "ЕЩЕ\n  /pure/"))
        assertNotNull(engine.comment(board, "a.b"))
        assertNull(engine.comment(board, "axb"))
    }

    @Test fun scopeDisabledAndTargetAreRespected() {
        assertNull(ContentFilterEngine(listOf(ordered.copy(enabled = false))).comment(board, "Храм ГОЛОВЫ"))
        assertNull(ContentFilterEngine(listOf(ordered.copy(boards = setOf("sex")))).thread(board, "Храм ГОЛОВЫ", ""))
        assertNull(
            ContentFilterEngine(
                listOf(ordered.copy(targets = ordered.targets.copy(comments = false))),
            ).comment(board, "Храм ГОЛОВЫ"),
        )
    }

    @Test fun absentSubjectDoesNotUseOriginalPostInTitleMode() {
        val rule = ordered.copy(targets = ordered.targets.copy(fields = ContentFilterFields.SUBJECT))
        val engine = ContentFilterEngine(listOf(rule))
        assertNull(engine.thread(board, null, "Храм ГОЛОВЫ"))
    }

    @Test fun emptyConditionsNeverHide() {
        assertNull(
            ContentFilterEngine(
                listOf(ordered.copy(condition = ordered.condition.copy(values = emptyList()))),
            ).comment(board, "anything"),
        )
        assertNull(
            ContentFilterEngine(
                listOf(ordered.copy(condition = ordered.condition.copy(values = listOf("  ")))),
            ).thread(board, "anything", ""),
        )
    }

    @Test
    fun eitherFieldPrefersSubjectAndFallsBackToOriginalPost() {
        val engine = ContentFilterEngine(listOf(ordered))
        assertEquals(FilterMatchField.SUBJECT, engine.threadMatch(board, "Храм ГОЛОВЫ", "Храм ГОЛОВЫ")?.field)
        assertEquals(
            FilterMatchField.ORIGINAL_POST,
            engine.threadMatch(board, "Другой заголовок", "Храм ГОЛОВЫ")?.field,
        )
        assertNull(engine.threadMatch(board, "Храм", "ГОЛОВЫ"))
    }
}
