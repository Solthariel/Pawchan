package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogAttachmentSession
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAttachmentSessionTest {
    @Test fun choiceGroupStaysVisibleUnderViewer() {
        val session = CatalogAttachmentSession()
        val group = emptyList<com.solthariel.pawchan.media.MediaAttachmentUi>()
        session.open(group)
        assertTrue(session.visible)
        assertSame(group, session.choices)
    }

    @Test fun onlyExplicitDismissClearsChoiceSession() {
        val session = CatalogAttachmentSession()
        session.open(emptyList())
        session.dismiss()
        assertNull(session.choices)
        assertFalse(session.visible)
    }

    @Test fun openingAnotherGroupReplacesOnlyThisSession() {
        val session = CatalogAttachmentSession()
        val other = CatalogAttachmentSession()
        session.open(emptyList())
        assertNull(other.choices)
        assertTrue(session.visible)
    }
}
