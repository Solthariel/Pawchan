package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CurrentFilterRecipesTest {
    private val board = BoardAddress(checkNotNull(SiteId.parse("dvach")), checkNotNull(BoardId.parse("b")))

    @Test fun obsoleteModesKeepTheirDataButCannotHidePosts() {
        for (mode in listOf(ContentFilterMode.ANY_PHRASE, ContentFilterMode.ALL_WORDS, ContentFilterMode.ORDERED)) {
            val rule = rule("кот").copy(condition = ContentFilterConditionSpec(mode, listOf("кот")))
            assertNotNull(contentFilterValidationError(rule))
            assertNull(ContentFilterEngine(listOf(rule)).comment(board, "кот"))
        }
    }

    @Test fun bothWordsRequireANonemptyMatchAndRespectAnExceptionAnywhere() {
        val word = "[\\p{L}\\p{M}\\p{N}_]"
        val expression =
            """\A(?!.*(?<!$word)приют(?!$word))""" +
                """(?=.*(?<!$word)кот(?!$word))(?=.*(?<!$word)собака(?!$word)).+"""
        val engine = ContentFilterEngine(listOf(rule(expression)))
        for (text in listOf("КОТ\nсобака", "Собака и кот", "кот собака приютский")) {
            assertNotNull(text, engine.comment(board, text))
        }
        val visible =
            listOf(
                "кот",
                "котик собака",
                "кот собака\nПРИЮТ",
                "приют кот собака",
                "Xкот собака",
                "кот собака_1",
            )
        for (text in visible) {
            assertNull(text, engine.comment(board, text))
        }
        assertNull(ContentFilterEngine(listOf(rule("\\A(?=.*кот)(?=.*собака)"))).comment(board, "кот собака"))
    }

    @Test fun prefixExceptionWorksBeforeAfterAndAcrossNewlines() {
        val expression = """\A(?!.*(?<![\p{L}\p{M}\p{N}_])известн).*?(?<![\p{L}\p{M}\p{N}_])беларус"""
        val engine = ContentFilterEngine(listOf(rule(expression)))
        for (text in listOf("Беларусы", "Неизвестный беларус", "Беларус известен")) {
            assertNotNull(text, engine.comment(board, text))
        }
        for (text in listOf("известного беларуса", "Беларус\nизвестный", "Небеларус", "_беларус", "1беларус")) {
            assertNull(text, engine.comment(board, text))
        }
    }

    @Test fun currentConditionsStillEnforceTheirWorkAndSizeLimits() {
        assertNotNull(contentFilterValidationError(rule("a".repeat(4097))))
        val exact = rule("a").copy(condition = ContentFilterConditionSpec(ContentFilterMode.EXACT_PHRASE, listOf("a")))
        assertNull(contentFilterValidationError(exact))
        assertNotNull(
            contentFilterValidationError(
                exact.copy(
                    condition = exact.condition.copy(values = listOf("a".repeat(32769))),
                ),
            ),
        )
    }

    private fun rule(expression: String): ContentFilterRule =
        ContentFilterRule(
            "r",
            "dvach",
            targets = ContentFilterTargets(comments = true),
            condition = ContentFilterConditionSpec(ContentFilterMode.REGEX, listOf(expression)),
        )
}
