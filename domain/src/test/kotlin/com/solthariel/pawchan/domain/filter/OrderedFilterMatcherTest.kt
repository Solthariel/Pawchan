package com.solthariel.pawchan.domain.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class OrderedFilterMatcherTest {
    @Test fun boundedMatcherAgreesWithExhaustiveSearchIncludingOverlappingOccurrences() {
        val random = Random(42)
        repeat(5_000) {
            val text = buildString { repeat(random.nextInt(1, 25)) { append("abc"[random.nextInt(3)]) } }
            val fragments = listOf("a", "b", "aa", "ab", "ba", "abc", "bc")
            val values = List(random.nextInt(1, 6)) { fragments.random(random) }
            val gap = if (random.nextBoolean()) null else random.nextInt(4)
            val result = OrderedFilterMatcher(values, gap).ranges(text)
            assertEquals("$text $values $gap", exhaustive(text, values, gap), result != null)
            if (result != null) {
                assertEquals(values, result.map { text.substring(it) })
                assertTrue(
                    result.zipWithNext().all { (a, b) ->
                        a.last < b.first && (gap == null || b.first - a.last - 1 <= gap)
                    },
                )
            }
        }
    }

    @Test fun laterReachableChainSurvivesAnEarlierDeadEnd() {
        assertNotNull(OrderedFilterMatcher(listOf("a", "b", "c"), 1).ranges("a---a b c"))
        assertNull(OrderedFilterMatcher(listOf("ab", "bc"), 0).ranges("abc"))
        assertNotNull(OrderedFilterMatcher(listOf("ab", "bc"), 0).ranges("abbc"))
    }

    @Test(timeout = 5_000)
    fun longTextAndManyValuesDoNotCopyEveryPrefixOrTruncateTheTail() {
        val values = List(MAX_FILTER_VALUES) { "a" }
        val text = "x".repeat(250_000) + "a".repeat(MAX_FILTER_VALUES)
        val result = requireNotNull(OrderedFilterMatcher(values, 0).ranges(text))
        assertEquals(MAX_FILTER_VALUES, result.size)
        assertEquals(250_000, result.first().first)
        assertEquals(text.lastIndex, result.last().last)
        assertNull(OrderedFilterMatcher(values + "missing", 0).ranges(text))
    }

    @Test fun conditionLimitsAreExplicitAndDoNotRejectOrdinaryRules() {
        val rule = ContentFilterRule("id", "dvach", condition = ContentFilterConditionSpec(values = List(128) { "a" }))
        assertNull(contentFilterValidationError(rule))
        val tooMany = rule.copy(condition = rule.condition.copy(values = List(129) { "a" }))
        assertNotNull(contentFilterValidationError(tooMany))
        val oversized = rule.copy(condition = rule.condition.copy(values = listOf("a".repeat(32_769))))
        assertNotNull(contentFilterValidationError(oversized))
        val exceptions = rule.copy(options = ContentFilterOptions(exceptions = List(129) { "x" }))
        assertNotNull(contentFilterValidationError(exceptions))
    }

    private fun exhaustive(
        text: String,
        values: List<String>,
        gap: Int?,
        stage: Int = 0,
        start: Int = 0,
    ): Boolean {
        if (stage == values.size) return true
        return findChain(text, values, gap, stage, start)
    }

    private fun findChain(
        text: String,
        values: List<String>,
        gap: Int?,
        stage: Int,
        start: Int,
    ): Boolean {
        val value = values[stage]
        var offset = text.indexOf(value, start)
        val end = if (stage == 0 || gap == null) text.length else start + gap
        while (offset in 0..end) {
            if (exhaustive(text, values, gap, stage + 1, offset + value.length)) return true
            offset = text.indexOf(value, offset + 1)
        }
        return false
    }
}
