package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterAdvancedTest {
    private val board = BoardAddress(checkNotNull(SiteId.parse("2ch")), checkNotNull(BoardId.parse("b")))
    private val base =
        ContentFilterRule(
            "rule",
            "2ch",
            targets = ContentFilterTargets(comments = true),
            condition = ContentFilterConditionSpec(values = listOf("Храм", "ГОЛОВЫ")),
        )

    @Test
    fun allWordsHaveUnicodeBoundariesAndIgnoreOrder() {
        val rule = base.copy(condition = base.condition.copy(mode = ContentFilterMode.ALL_WORDS))
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.comment(board, "головы, а потом ХРАМ"))
        assertNull(engine.comment(board, "храмовый головы"))
        assertNull(engine.comment(board, "_храм головы"))
        val partial =
            base.copy(
                options = ContentFilterOptions(wholeWords = false),
                condition = base.condition.copy(mode = ContentFilterMode.ALL_WORDS),
            )
        assertNotNull(ContentFilterEngine(listOf(partial)).comment(board, "храмовый головы"))
    }

    @Test
    fun exceptionsAreLocalToMatchedFieldAndRule() {
        val rule =
            base.copy(
                options = ContentFilterOptions(exceptions = listOf("пример")),
                condition = base.condition.copy(mode = ContentFilterMode.ORDERED),
            )
        val engine = ContentFilterEngine(listOf(rule))
        assertNull(engine.comment(board, "Храм головы: пример"))
        assertNotNull(engine.thread(board, "Храм головы", "пример"))
        assertNotNull(engine.thread(board, "Храм головы пример", "Храм головы"))
        assertNotNull(ContentFilterEngine(listOf(rule, base.copy(id = "second"))).comment(board, "Храм головы пример"))
    }

    @Test
    fun orderedGapFindsLaterValidChain() {
        val rule =
            base.copy(
                options = ContentFilterOptions(maxGap = 1),
                condition = base.condition.copy(mode = ContentFilterMode.ORDERED, values = listOf("a", "b", "c")),
            )
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.comment(board, "a---a b c"))
        assertNull(engine.comment(board, "a -- b c"))
        assertNull(engine.comment(board, "c b a"))
        assertNotNull(
            ContentFilterEngine(listOf(rule.copy(options = ContentFilterOptions())))
                .comment(board, "a\n\n-- b\n-- c"),
        )
    }

    @Test
    fun rangesReferToOriginalTextAfterNormalization() {
        val rule = base.copy(condition = base.condition.copy(values = listOf("еще храм")))
        val text = "  ЕЩЁ\n  ХРАМ!"
        val match = checkNotNull(ContentFilterEngine(listOf(rule)).commentMatch(board, text))
        assertEquals("ЕЩЁ\n  ХРАМ", text.substring(match.ranges.single()))
        assertEquals(FilterMatchField.COMMENT, match.field)
    }

    @Test
    fun regexIsExplicitMultilineAndValidated() {
        val rule =
            base.copy(
                condition = base.condition.copy(mode = ContentFilterMode.REGEX, values = listOf("храм.*головы")),
            )
        assertNotNull(ContentFilterEngine(listOf(rule)).comment(board, "ХРАМ\nГОЛОВЫ"))
        val invalid = rule.copy(condition = rule.condition.copy(values = listOf("[")))
        assertNotNull(contentFilterValidationError(invalid))
        assertNull(ContentFilterEngine(listOf(invalid)).comment(board, "anything"))
        val emptyMatch = rule.copy(condition = rule.condition.copy(values = listOf("^")))
        assertNull(ContentFilterEngine(listOf(emptyMatch)).comment(board, "anything"))
        val literal = base.copy(condition = base.condition.copy(values = listOf("a.b")))
        assertNull(ContentFilterEngine(listOf(literal)).comment(board, "axb"))
    }

    @Test
    fun expiredDisabledAndOtherSitesDoNotMatch() {
        val expired = base.copy(options = ContentFilterOptions(expiresAtMillis = 1L))
        assertNull(ContentFilterEngine(listOf(expired)).comment(board, "Храм головы"))
        assertNull(ContentFilterEngine(listOf(base.copy(site = "other"))).comment(board, "Храм головы"))
        assertNull(ContentFilterEngine(listOf(base.copy(enabled = false))).comment(board, "Храм головы"))
        assertEquals("Моё правило", base.copy(options = ContentFilterOptions(customLabel = "Моё правило")).label)
    }

    @Test
    fun orderedChainPreservesRangesAsElements() {
        val rule =
            base.copy(
                condition =
                    ContentFilterConditionSpec(
                        mode = ContentFilterMode.ORDERED,
                        values = listOf("a", "b", "c"),
                    ),
            )
        val match = checkNotNull(ContentFilterEngine(listOf(rule)).commentMatch(board, "a b c"))
        assertEquals(listOf(0..0, 2..2, 4..4), match.ranges)
    }

    @Test(timeout = 2000)
    fun pathologicalRegexHasFiniteWorkBudget() {
        val rule = base.copy(condition = base.condition.copy(mode = ContentFilterMode.REGEX, values = listOf("(a+)+b")))
        assertNull(ContentFilterEngine(listOf(rule)).comment(board, "a".repeat(4000)))
    }
}
