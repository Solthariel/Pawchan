package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterSimpleTest {
    @Test fun phraseIsLiteralIncludingPunctuationAndWhitespace() {
        val engine = ContentFilterEngine(listOf(builderRule(terms = listOf("скидка сегодня"))))
        assertNotNull(engine.comment(builderBoard, "Большая СКИДКА СЕГОДНЯ!"))
        listOf("скидки сегодня", "скидка только сегодня", "скидка\nсегодня", "скидка  сегодня").forEach {
            assertNull(it, engine.comment(builderBoard, it))
        }
    }

    @Test fun regexCharactersAreNeverExecuted() {
        val engine = ContentFilterEngine(listOf(builderRule(terms = listOf("(a+)+$"), boundary = FilterBoundary.PART)))
        assertNotNull(engine.comment(builderBoard, "буквально (a+)+$"))
        assertNull(engine.comment(builderBoard, "a".repeat(100_000)))
    }

    @Test fun anyTermRequiresOnlyOneAndAllTermsRequiresEveryOne() {
        val any = builderRule(ContentFilterMode.ANY_TERM, listOf("кот", "собака"))
        val all = any.copy(condition = any.condition.copy(mode = ContentFilterMode.ALL_TERMS))
        assertNotNull(ContentFilterEngine(listOf(any)).comment(builderBoard, "СОБАКА"))
        assertNull(ContentFilterEngine(listOf(all)).comment(builderBoard, "СОБАКА"))
        assertNotNull(ContentFilterEngine(listOf(all)).comment(builderBoard, "собака\nи кот"))
    }

    @Test fun multiwordTermIsNotSplitOnSpacesOrComma() {
        val rule = builderRule(ContentFilterMode.ANY_TERM, listOf("кот дома", "собака, рядом"))
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.comment(builderBoard, "кот дома"))
        assertNull(engine.comment(builderBoard, "кот"))
        assertNull(engine.comment(builderBoard, "собака рядом"))
    }
}
