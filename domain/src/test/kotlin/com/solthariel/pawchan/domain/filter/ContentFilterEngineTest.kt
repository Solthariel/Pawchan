package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterEngineTest {
    @Test fun phraseMatchesBothPostKindsAndOriginalRanges() {
        val rule = builderRule()
        val engine = ContentFilterEngine(listOf(rule))
        assertEquals(rule, engine.thread(builderBoard, "КОТ", "обычный текст"))
        assertEquals(rule, engine.comment(builderBoard, "Вот КОТ!"))
        assertEquals(listOf(4..6), engine.commentMatch(builderBoard, "Вот КОТ!")?.ranges)
    }

    @Test fun bothWordsMayBeSplitBetweenSubjectAndBody() {
        val rule = builderRule(ContentFilterMode.ALL_TERMS, listOf("кот", "собака"))
        val match = ContentFilterEngine(listOf(rule)).threadMatch(builderBoard, "КОТ", "собака")!!
        assertEquals(FilterMatchField.ORIGINAL_POST, match.field)
        assertEquals(listOf(0..5), match.ranges)
    }

    @Test fun exceptionInBodyCancelsSubjectMatchAndViceVersa() {
        val engine = ContentFilterEngine(listOf(builderRule(exceptions = listOf("приют"))))
        assertNull(engine.thread(builderBoard, "кот", "приют"))
        assertNull(engine.thread(builderBoard, "приют", "кот"))
        assertNull(engine.comment(builderBoard, "КОТ\nПРИЮТ"))
    }

    @Test fun disabledAndWrongSiteNeverMatch() {
        val rule = builderRule()
        assertNull(ContentFilterEngine(listOf(rule.copy(enabled = false))).comment(builderBoard, "кот"))
        assertNull(ContentFilterEngine(listOf(rule.copy(site = "other"))).comment(builderBoard, "кот"))
    }

    @Test fun firstMatchingRuleKeepsStableIdentity() {
        val first = builderRule()
        val second = first.copy(id = "second")
        assertEquals(first, ContentFilterEngine(listOf(first, second)).comment(builderBoard, "кот"))
    }
}
