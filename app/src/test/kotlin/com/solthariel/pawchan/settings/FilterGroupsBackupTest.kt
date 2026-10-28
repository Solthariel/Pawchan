package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterConditionSpec
import com.solthariel.pawchan.domain.filter.ContentFilterMode
import com.solthariel.pawchan.domain.filter.ContentFilterTargets
import com.solthariel.pawchan.navigation.defaultBoardAddress
import org.junit.Assert.assertEquals
import org.junit.Test

class FilterGroupsBackupTest {
    @Test fun groupsScopesAndExceptionsRoundTripInCurrentBackup() {
        val rule =
            newContentFilterRule(defaultBoardAddress()).copy(
                targets = ContentFilterTargets(comments = false),
                condition = ContentFilterConditionSpec(ContentFilterMode.ALL_TERMS, listOf("пустите", "фап, fap")),
            )
        assertEquals(listOf(rule), decodeContentFilterBackup(encodeContentFilterBackup(listOf(rule))))
        val draft = ContentFilterDraft(rule)
        assertEquals(FilterPostScope.OP.name, draft.postScope)
        assertEquals(rule, draft.rule(rule))
    }

    @Test(expected = IllegalArgumentException::class)
    fun previousBuilderBackupIsRejectedRatherThanReinterpreted() {
        decodeContentFilterBackup("format=pawchan-autohide\nversion=2\nrules=0")
    }
}
