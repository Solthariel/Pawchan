package com.solthariel.pawchan.domain.posting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PostingUploadBudgetTest {
    @Test fun aggregateBudgetIsIndependentOfIndividualFileAndSelectionCaps() {
        val rules = PostingRules(maxFileBytes = 30, maxTotalFileBytes = 40)
        val file = ReplyAttachment("clip.mp4", "video/mp4", ByteArray(20))
        assertTrue(rules.accepts(listOf(file, file)))
        val larger = file.copy(content = ByteArray(21))
        assertFalse(rules.accepts(listOf(file, larger)))
        assertTrue(rules.acceptsSelection(listOf(file, larger)))
        assertFalse(rules.accepts(listOf(file.copy(content = ByteArray(31)))))
    }

    @Test fun serverAccountBudgetOverridesBoardBudgetWithoutChangingMemorySafetyCap() {
        val rules = PostingRules(maxTotalFileBytes = 40).withAccountLimits(AccountUploadLimits(8, 50))
        assertEquals(50, rules.maxTotalFileBytes)
        assertEquals(ReplyAttachment.MAX_ATTACHMENT_BYTES, rules.maxFileBytes)
        assertEquals(8, rules.maxFiles)
    }

    @Test fun progressPercentageDoesNotRepresentServerAcceptance() {
        assertEquals(40, PostingUploadProgress(4, 10).percent)
        assertFalse(PostingUploadProgress(10, 10).awaitingResponse)
        assertTrue(PostingUploadProgress(10, 10, true).awaitingResponse)
        assertEquals(0, PostingUploadProgress(0, 0).percent)
    }
}
