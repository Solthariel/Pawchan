package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

class ContentFilterCodecTest {
    @Test fun roundTripPreservesLiteralPunctuation() {
        val rules =
            listOf(
                ContentFilterRule(
                    "1",
                    "2ch",
                    setOf("b", "sex"),
                    targets = ContentFilterTargets(comments = true),
                    condition = ContentFilterConditionSpec(values = listOf("ещё /pure/", "a,b", "[x]\t(y)")),
                ),
            )
        assertEquals(rules, decodeContentFilters(encodeContentFilters(rules)))
    }

    @Test fun invalidRecordDoesNotDiscardValidOnes() {
        val rules =
            listOf(
                ContentFilterRule(
                    "1",
                    "2ch",
                    condition = ContentFilterConditionSpec(values = listOf("фраза")),
                ),
            )
        assertEquals(rules, decodeContentFilters("invalid\n" + encodeContentFilters(rules)))
        assertEquals(emptyList<ContentFilterRule>(), decodeContentFilters(""))
    }

    @Test
    fun versionTwoPreservesMultilineRegexAndOptions() {
        val rules =
            listOf(
                ContentFilterRule(
                    "2",
                    "2ch",
                    options =
                        ContentFilterOptions(
                            "Имя",
                            false,
                            12,
                            listOf("a,b", "ещё\nпример", "[x]\t(y)"),
                            123456L,
                        ),
                    condition =
                        ContentFilterConditionSpec(
                            values = listOf("храм.*\nголовы"),
                            mode = ContentFilterMode.REGEX,
                        ),
                ),
            )
        assertEquals(rules, decodeContentFilters(encodeContentFilters(rules)))
    }

    @Test
    fun legacyVersionOneIsStillReadable() {
        val fields = listOf("1", "legacy", "2ch", "b", "true", "true", "false", "EITHER", "ORDERED", "Храм\nГОЛОВЫ")
        val encoded = fields.joinToString("\t") { Base64.getEncoder().encodeToString(it.toByteArray(Charsets.UTF_8)) }
        val expected =
            ContentFilterRule(
                "legacy",
                "2ch",
                setOf("b"),
                condition =
                    ContentFilterConditionSpec(
                        mode = ContentFilterMode.ORDERED,
                        values = listOf("Храм", "ГОЛОВЫ"),
                    ),
            )
        assertEquals(listOf(expected), decodeContentFilters(encoded))
    }
}
