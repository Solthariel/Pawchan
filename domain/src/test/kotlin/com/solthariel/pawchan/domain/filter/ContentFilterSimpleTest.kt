package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterSimpleTest {
    private val board = BoardAddress(checkNotNull(SiteId.parse("dvach")), checkNotNull(BoardId.parse("b")))
    private val phrase =
        ContentFilterRule(
            "simple",
            board.site.value,
            setOf(board.board.value),
            targets = ContentFilterTargets(comments = true),
            condition = ContentFilterConditionSpec(ContentFilterMode.EXACT_PHRASE, listOf("голова дай денег")),
        )

    @Test
    fun exactPhraseIsContainedButNeverInflectedReorderedOrCompletedInsideWords() {
        val engine = ContentFilterEngine(listOf(phrase))
        assertNotNull(engine.comment(board, "Ну, ГОЛОВА  \nДАЙ денег!"))
        listOf("головы дайте деньги", "голова денег дай", "голова, дай денег", "суперголова дай денег").forEach {
            assertNull(it, engine.comment(board, it))
        }
        assertNull(engine.comment(board, "голова дай денегами"))
    }

    @Test
    fun exactPhrasePreservesOriginalSourceRangesAndFindsLaterValidOccurrence() {
        val text = "суперголова дай денег; ГОЛОВА  \nДАЙ денег!"
        val match = checkNotNull(ContentFilterEngine(listOf(phrase)).commentMatch(board, text))
        assertEquals("ГОЛОВА  \nДАЙ денег", text.substring(match.ranges.single()))
    }

    @Test
    fun exactPhraseNormalizesYoAndWhitespaceButKeepsPunctuationLiteral() {
        val condition = ContentFilterConditionSpec(ContentFilterMode.EXACT_PHRASE, listOf("ещё, хлеб"))
        val rule = phrase.copy(condition = condition)
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.comment(board, "ЕЩЕ,\u00a0 ХЛЕБ!"))
        assertNull(engine.comment(board, "ещё хлеб"))
        assertNull(engine.comment(board, "еще, хлебный"))
        val punctuation = rule.copy(condition = rule.condition.copy(values = listOf("[x]")))
        assertNotNull(ContentFilterEngine(listOf(punctuation)).comment(board, "a[x]b"))
    }

    @Test
    fun allWordsStayInOneFieldOrOnePostAndHaveWholeWordEdges() {
        val condition =
            ContentFilterConditionSpec(
                ContentFilterMode.REGEX,
                listOf(
                    """\A(?=.*(?<![\p{L}\p{N}_])голова(?![\p{L}\p{N}_]))""" +
                        """(?=.*(?<![\p{L}\p{N}_])дай(?![\p{L}\p{N}_]))""" +
                        """(?=.*(?<![\p{L}\p{N}_])денег(?![\p{L}\p{N}_])).+""",
                ),
            )
        val rule = phrase.copy(condition = condition)
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.comment(board, "Денег! Потом голова, потом дай."))
        assertNull(engine.comment(board, "голова передай денег"))
        assertNull(engine.thread(board, "голова", "дай денег"))
        assertNull(engine.comment(board, "голова"))
        assertNull(engine.comment(board, "дай денег"))
    }

    @Test
    fun originalSubjectAndBodyAreIndependentAndReplyTargetsAreOptional() {
        val engine = ContentFilterEngine(listOf(phrase))
        assertNotNull(engine.thread(board, "голова дай денег", "обычный текст"))
        assertNotNull(engine.thread(board, "обычная тема", "голова дай денег"))
        assertNull(engine.thread(board, "голова дай", "денег"))
        val opOnly = phrase.copy(targets = phrase.targets.copy(comments = false))
        assertNull(ContentFilterEngine(listOf(opOnly)).comment(board, "голова дай денег"))
    }

    @Test
    fun placementNeverTurnsACatalogRuleIntoAnOpenedThreadRule() {
        val catalog = phrase.copy(targets = phrase.targets.copy(thread = false))
        val thread = phrase.copy(targets = phrase.targets.copy(catalog = false))
        val text = phrase.values.single()
        assertNotNull(ContentFilterEngine(listOf(catalog), ContentFilterPlace.CATALOG).thread(board, null, text))
        assertNull(ContentFilterEngine(listOf(catalog), ContentFilterPlace.THREAD).thread(board, null, text))
        assertNull(ContentFilterEngine(listOf(thread), ContentFilterPlace.CATALOG).thread(board, null, text))
        assertNotNull(ContentFilterEngine(listOf(thread), ContentFilterPlace.THREAD).comment(board, text))
        val noPlace = phrase.copy(targets = phrase.targets.copy(catalog = false, thread = false))
        assertNotNull(contentFilterValidationError(noPlace))
    }

    @Test
    fun legacyRulesNeverExecuteAndLogicalBoardIsolationRemainsUnchanged() {
        val legacy = phrase.copy(condition = ContentFilterConditionSpec(ContentFilterMode.ANY_PHRASE, listOf("хлеб")))
        assertNull(ContentFilterEngine(listOf(legacy)).comment(board, "хлебный"))
        val otherBoard = board.copy(board = checkNotNull(BoardId.parse("po")))
        assertNull(ContentFilterEngine(listOf(phrase)).comment(otherBoard, phrase.values.single()))
        val otherSite = board.copy(site = checkNotNull(SiteId.parse("another")))
        assertNull(ContentFilterEngine(listOf(phrase)).comment(otherSite, phrase.values.single()))
    }
}
