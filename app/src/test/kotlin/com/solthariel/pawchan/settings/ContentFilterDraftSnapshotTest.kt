package com.solthariel.pawchan.settings

import com.solthariel.pawchan.domain.filter.ContentFilterRule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContentFilterDraftSnapshotTest {
    @Test
    fun snapshotDetectsInvalidUnparsedInputAndReturnsToBaseline() {
        val draft = ContentFilterDraft(ContentFilterRule("id", "2ch.hk"))
        val initial = draft.snapshot()
        draft.threadIds = "not-a-number"
        assertNotEquals(initial, draft.snapshot())
        draft.threadIds = ""
        assertEquals(initial, draft.snapshot())
        draft.enabled = !draft.enabled
        assertNotEquals(initial, draft.snapshot())
    }
}
