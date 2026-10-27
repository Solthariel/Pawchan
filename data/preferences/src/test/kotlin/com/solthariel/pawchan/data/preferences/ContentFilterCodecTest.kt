package com.solthariel.pawchan.data.preferences

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterOptions
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.domain.filter.FilterBoundary
import com.solthariel.pawchan.domain.filter.FilterTextMatch
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Base64

class ContentFilterCodecTest {
    @Test fun newModesScopesFlagsAndUtf8RoundTrip() {
        for (mode in listOf(ContentFilterMode.PHRASE, ContentFilterMode.ANY_TERM, ContentFilterMode.ALL_TERMS)) {
            for (boundary in FilterBoundary.entries) {
                val rule =
                    ContentFilterRule(
                        "id",
                        "dvach",
                        setOf("b", "vg"),
                        enabled = false,
                        targets = ContentFilterTargets(threadIds = setOf(42, 43)),
                        condition = ContentFilterConditionSpec(mode, listOf("Ёжик\n\t=;:")),
                        options =
                            ContentFilterOptions(
                                exceptions = listOf("дом", "x;y"),
                                textMatch = FilterTextMatch(boundary, true),
                            ),
                    )
                assertEquals(listOf(rule), decodeContentFilters(encodeContentFilters(listOf(rule))))
            }
        }
    }

    @Test fun badRecordDoesNotDiscardValidNewRecords() {
        val rule = ContentFilterRule("id", "dvach", condition = ContentFilterConditionSpec(values = listOf("кот")))
        val encoded = encodeContentFilters(listOf(rule))
        assertEquals(listOf(rule), decodeContentFilters("not-base64\n$encoded\n$encoded\tbroken").distinct())
    }
}
