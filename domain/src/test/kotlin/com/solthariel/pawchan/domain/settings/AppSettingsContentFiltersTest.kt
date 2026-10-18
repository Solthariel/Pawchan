package com.solthariel.pawchan.domain.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsContentFiltersTest {
    private val rules =
        listOf(ContentFilterRule("rule", "dvach", condition = ContentFilterConditionSpec(values = listOf("OP"))))

    @Test
    fun defaultsExposeEmptyRules() {
        assertEquals(emptyList<ContentFilterRule>(), AppSettings().contentFilters)
    }

    @Test
    fun ruleUpdatePreservesOtherPreferencesAndOriginalValue() {
        val previous =
            AppSettings(
                behavior =
                    AppBehavior(
                        videoStartMuted = true,
                        updateSavedCopies = false,
                        vocarooDns = VocarooDns(VocarooDnsProvider.CUSTOM, "https://example.org/dns-query"),
                    ),
            )
        val next = previous.withContentFilters(rules)
        assertEquals(rules, next.contentFilters)
        assertEquals(rules, next.behavior.contentFilters)
        assertEquals(previous.copy(behavior = previous.behavior.copy(contentFilters = rules)), next)
        assertEquals(emptyList<ContentFilterRule>(), previous.contentFilters)
        assertEquals(previous, next.withContentFilters(emptyList()))
    }
}
