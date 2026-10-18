package com.solthariel.pawchan

import com.solthariel.pawchan.composer.ComposerDraftState
import com.solthariel.pawchan.composer.appendReplyInsertion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostMenuComposerInsertionTest {
    @Test
    fun insertionPreservesDraftAndRefusesOverflowWithoutTruncation() {
        val draft = ComposerDraftState(comment = "Черновик", email = "mail")
        assertEquals("Черновик\n>>7\n", appendReplyInsertion(draft, ">>7\n", 100)?.comment)
        assertEquals("mail", appendReplyInsertion(draft, ">>7\n", 100)?.email)
        assertNull(appendReplyInsertion(draft, ">>7\n", 4))
    }
}
