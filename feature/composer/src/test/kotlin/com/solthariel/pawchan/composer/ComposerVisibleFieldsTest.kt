package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.settings.ComposerField
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ComposerVisibleFieldsTest {
    private val draft =
        ComposerDraftState(
            comment = "text",
            subject = "subject",
            email = "mail",
            options = PostingOptions("name", sage = true, threadAuthor = true, icon = 3, tag = "tag"),
        )

    @Test fun hidingTextFieldsDoesNotDisablePostingIcons() {
        val clean = draft.withVisibleFields(ComposerField.entries.toSet())
        assertEquals("", clean.email)
        assertEquals("", clean.subject)
        assertEquals("", clean.options.name)
        assertEquals("", clean.options.tag)
        assertEquals("text", clean.comment)
        assertEquals(3, clean.options.icon)
        assertTrue(clean.options.sage)
        assertTrue(clean.options.threadAuthor)
    }

    @Test fun noHiddenFieldsLeavesDraftUntouched() {
        assertEquals(draft, draft.withVisibleFields(emptySet()))
    }
}
