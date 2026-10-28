package com.solthariel.pawchan.domain.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class FilterConditionGroupsTest {
    @Test fun everyRowNeedsOneAlternativeNotEveryAlternative() {
        val rule = builderRule(ContentFilterMode.ALL_TERMS, listOf("пустите", "фап, fap"))
        val engine = ContentFilterEngine(listOf(rule))
        listOf("пустите в фап", "ПУСТИТЕ меня в Fap", "в /fap/ меня пустите", "пустите\nфап").forEach {
            assertNotNull(it, engine.comment(builderBoard, it))
        }
        listOf("пустите домой", "просто fap", "непустите в фап", "пустите в fapper").forEach {
            assertNull(it, engine.comment(builderBoard, it))
        }
    }

    @Test fun storedHardNewlinesStillMeanSeparateRequiredRows() {
        val rule = builderRule(ContentFilterMode.ALL_TERMS, listOf("пустите\nфап, fap"))
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.comment(builderBoard, "пустите меня в FAP"))
        assertNull(engine.comment(builderBoard, "пустите домой"))
        assertNull(engine.comment(builderBoard, "просто fap"))
    }

    @Test fun anyModeFlattensAllAlternativesAcrossRowsButKeepsMultiwordTerms() {
        val rule = builderRule(ContentFilterMode.ANY_TERM, listOf("два слова, фап", "fap"))
        val engine = ContentFilterEngine(listOf(rule))
        listOf("два слова", "фап", "FAP").forEach { assertNotNull(engine.comment(builderBoard, it)) }
        listOf("два", "слова", "два лишних слова").forEach { assertNull(engine.comment(builderBoard, it)) }
    }

    @Test fun literalPhraseKeepsCommaNewlineAndRegexPunctuation() {
        val rule = builderRule(terms = listOf("кот, собака\n(a+)+$"), boundary = FilterBoundary.PART)
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.comment(builderBoard, "кот, собака\n(a+)+$"))
        assertNull(engine.comment(builderBoard, "кот"))
        assertNull(engine.comment(builderBoard, "кот собака\n(a+)+$"))
    }

    @Test fun anyExceptionCancelsEveryGroupAcrossSubjectAndBody() {
        val rule =
            builderRule(
                ContentFilterMode.ALL_TERMS,
                listOf("пустите", "фап, fap"),
                exceptions = listOf("обсуждение, справка"),
            )
        val engine = ContentFilterEngine(listOf(rule))
        assertNotNull(engine.thread(builderBoard, "Пустите", "меня в FAP"))
        assertNull(engine.thread(builderBoard, "Пустите", "меня в фап. СПРАВКА"))
        assertNull(engine.thread(builderBoard, "Обсуждение: пустите", "в fap"))
    }

    @Test fun blankAlternativesNeverMatchEverythingAndCompiledTermsAreBounded() {
        assertEquals(listOf("кот", "собака"), filterAlternatives(" , кот, , собака, кот, "))
        val valid = builderRule(ContentFilterMode.ALL_TERMS, listOf(" , кот, ", "собака, "))
        assertNotNull(ContentFilterEngine(listOf(valid)).comment(builderBoard, "кот собака"))
        listOf(listOf(",, "), listOf(List(129) { "term$it" }.joinToString(","))).forEach { values ->
            assertNotNull(contentFilterValidationError(valid.copy(condition = valid.condition.copy(values = values))))
        }
    }

    @Test fun postKindsRestrictRealEngineMatchesAndNeverLeakAcrossKinds() {
        for ((op, comments) in listOf(true to false, false to true, true to true)) {
            val rule = builderRule().copy(targets = ContentFilterTargets(threads = op, comments = comments))
            val engine = ContentFilterEngine(listOf(rule))
            assertNull(contentFilterValidationError(rule))
            assertEquals(op, engine.thread(builderBoard, "кот", "") != null)
            assertEquals(comments, engine.comment(builderBoard, "кот") != null)
        }
    }

    @Test fun highlightRangesPointToRealChosenAlternativesWithUnicodeAndPunctuation() {
        val rule = builderRule(ContentFilterMode.ALL_TERMS, listOf("ПУСТИТЕ, разрешите", "фап, fap"))
        val source = "Разрешите меня в /FAP/"
        val result = checkNotNull(ContentFilterEngine(listOf(rule)).commentMatch(builderBoard, source))
        assertEquals(listOf("Разрешите", "FAP"), result.ranges.map { source.substring(it) })
    }
}
