package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.appendReplyInsertion
import com.solthariel.pawchan.domain.posting.PostingOptions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReplyInsertionTest {
    @Test
    fun emptyDraftDoesNotGetALeadingSeparator() {
        assertEquals(">>7\n", appendReplyInsertion(ComposerDraftState(), ">>7\n", 4)?.comment)
    }

    @Test
    fun existingTrailingNewlineIsNotDuplicated() {
        val draft = ComposerDraftState(comment = "Текст\n")
        assertEquals("Текст\n>>7\n", appendReplyInsertion(draft, ">>7\n", 10)?.comment)
    }

    @Test
    fun exactLimitIsAcceptedButOverflowDoesNotMutateTheDraft() {
        val draft = ComposerDraftState(comment = "abc")
        assertEquals("abc\nx", appendReplyInsertion(draft, "x", 5)?.comment)
        assertNull(appendReplyInsertion(draft, "x", 4))
        assertEquals("abc", draft.comment)
    }

    @Test
    fun insertionOnlyChangesTheComment() {
        val draft =
            ComposerDraftState(
                subject = "subject",
                comment = "abc",
                email = "mail",
                resetId = 9,
                options = PostingOptions("name", sage = true),
            )
        assertEquals(draft.copy(comment = "abc\nx"), appendReplyInsertion(draft, "x", 5))
    }
}
