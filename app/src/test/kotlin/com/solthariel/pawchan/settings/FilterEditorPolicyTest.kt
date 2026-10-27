package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.FilterBoundary
import com.solthariel.pawchan.domain.filter.FilterTextMatch
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FilterEditorPolicyTest {
    @Test fun newAndRestoredDraftsSaveWithoutCaseSensitivity() {
        val base = newContentFilterRule(defaultBoardAddress(), "VIP")
        val old = base.copy(options = base.options.copy(textMatch = FilterTextMatch(FilterBoundary.PART, true)))
        val next = ContentFilterDraft(old).rule(old)
        assertEquals(listOf("VIP"), next.values)
        assertEquals(FilterBoundary.PART, next.options.textMatch.boundary)
        assertFalse(next.options.textMatch.caseSensitive)
    }

    @Test fun removedPrefixChoiceBecomesWholeWordOnlyWhenTheEditorIsSaved() {
        val base = newContentFilterRule(defaultBoardAddress(), "кот")
        val old = base.copy(options = base.options.copy(textMatch = FilterTextMatch(FilterBoundary.PREFIX)))
        assertEquals(
            FilterBoundary.WORD,
            ContentFilterDraft(old)
                .rule(old)
                .options.textMatch.boundary,
        )
        assertEquals(FilterBoundary.PREFIX, old.options.textMatch.boundary)
    }
}
