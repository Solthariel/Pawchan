package com.solthariel.pawchan.domain.filter

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ContentFilterAdvancedTest {
    @Test fun boundariesDistinguishWholePrefixAndSubstring() {
        for (boundary in FilterBoundary.entries) {
            val engine = ContentFilterEngine(listOf(builderRule(boundary = boundary)))
            assertNotNull(engine.comment(builderBoard, "кот"))
            assertEquals(boundary != FilterBoundary.WORD, engine.comment(builderBoard, "котик") != null)
            assertEquals(boundary == FilterBoundary.PART, engine.comment(builderBoard, "скот") != null)
            assertEquals(boundary == FilterBoundary.PART, engine.comment(builderBoard, "Xкот") != null)
        }
    }

    @Test fun unicodeMarksNumbersUnderscoreAndAstralLettersAreWordCharacters() {
        val engine = ContentFilterEngine(listOf(builderRule()))
        listOf("кот_1", "1кот", "кот\u0301", "\uD801\uDC00кот", "кот²").forEach {
            assertNull(it, engine.comment(builderBoard, it))
        }
        assertNotNull(engine.comment(builderBoard, "«кот»"))
    }

    @Test fun caseSensitiveUsesExactCaseButDoesNotFoldYoToYe() {
        val insensitive = ContentFilterEngine(listOf(builderRule()))
        val sensitive = ContentFilterEngine(listOf(builderRule(sensitive = true)))
        assertNotNull(insensitive.comment(builderBoard, "КОТ"))
        assertNull(sensitive.comment(builderBoard, "КОТ"))
        assertNotNull(sensitive.comment(builderBoard, "кот"))
        assertNull(ContentFilterEngine(listOf(builderRule(terms = listOf("ёж")))).comment(builderBoard, "еж"))
    }

    @Test fun exceptionsShareCaseAndBoundaryPolicy() {
        val rule = builderRule(boundary = FilterBoundary.PREFIX, exceptions = listOf("приют"))
        assertNull(ContentFilterEngine(listOf(rule)).comment(builderBoard, "котик ПРИЮТский"))
        val sensitive = rule.copy(options = rule.options.copy(textMatch = FilterTextMatch(FilterBoundary.PREFIX, true)))
        assertNotNull(ContentFilterEngine(listOf(sensitive)).comment(builderBoard, "котик ПРИЮТский"))
        assertNull(ContentFilterEngine(listOf(sensitive)).comment(builderBoard, "котик приютский"))
    }

    @Test fun everyExceptionCancelsNotOnlyTheFirstOne() {
        val rule = builderRule(exceptions = listOf("приют", "дом"))
        assertNull(ContentFilterEngine(listOf(rule)).comment(builderBoard, "кот дома дом"))
    }
}
