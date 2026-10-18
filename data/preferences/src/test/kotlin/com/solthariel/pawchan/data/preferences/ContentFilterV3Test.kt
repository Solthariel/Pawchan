package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

class ContentFilterV3Test {
    @Test
    fun latestCodecPreservesEveryModePresentationScopeAndThreadNumbers() {
        val rules =
            ContentFilterMode.entries.map { mode ->
                ContentFilterRule(
                    mode.name,
                    "dvach",
                    targets = ContentFilterTargets(comments = true, catalog = false, threadIds = setOf(42, 43)),
                    condition = ContentFilterConditionSpec(mode, listOf("ещё\nфраза")),
                )
            }
        assertEquals(rules, decodeContentFilters(encodeContentFilters(rules)))
    }

    @Test
    fun realVersionTwoRecordsKeepOldAllWordsFragmentsAndOptions() {
        val fields =
            listOf(
                "2",
                "old",
                "dvach",
                "b",
                "true",
                "true",
                "true",
                "EITHER",
                "ALL_WORDS",
                items("два слова", "a,b"),
                "Имя",
                "false",
                "12",
                items("исключение"),
                "123456789",
            )
        val expected =
            ContentFilterRule(
                "old",
                "dvach",
                setOf("b"),
                targets = ContentFilterTargets(comments = true),
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_WORDS, listOf("два слова", "a,b")),
                options = ContentFilterOptions("Имя", false, 12, listOf("исключение"), 123456789L),
            )
        assertEquals(listOf(expected), decodeContentFilters(record(fields)))
        assertEquals(listOf(expected), decodeContentFilters(encodeContentFilters(decodeContentFilters(record(fields)))))
    }

    @Test
    fun malformedNewRecordDoesNotDiscardOtherSavedRules() {
        val rule = ContentFilterRule("ok", "dvach", condition = ContentFilterConditionSpec(values = listOf("хлеб")))
        val fields =
            listOf(
                "3",
                "bad",
                "dvach",
                "b",
                "true",
                "true",
                "false",
                "EITHER",
                "EXACT_PHRASE",
                items("хлеб"),
                "",
                "true",
                "",
                "",
                "",
                "not-a-boolean",
                "true",
            )
        assertEquals(listOf(rule), decodeContentFilters(record(fields) + "\n" + encodeContentFilters(listOf(rule))))
    }

    private fun items(vararg values: String): String = values.joinToString(";") { encode(it) }

    private fun record(fields: List<String>): String = fields.joinToString("\t") { encode(it) }

    private fun encode(value: String): String = Base64.getEncoder().encodeToString(value.toByteArray(Charsets.UTF_8))
}
