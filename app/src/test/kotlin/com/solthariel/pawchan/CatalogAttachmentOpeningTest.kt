package com.solthariel.pawchan

import com.solthariel.pawchan.catalog.CatalogAttachmentSession
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogAttachmentOpeningTest {
    @Test fun singleImageAndVideoBypassTheChooser() {
        for (viewer in listOf(AttachmentViewer.IMAGE, AttachmentViewer.VIDEO)) {
            val session = CatalogAttachmentSession()
            val item = attachment(viewer)
            var opened: MediaAttachmentUi? = null
            session.openOrChoose(listOf(item)) { opened = it }
            assertSame(item, opened)
            assertNull(session.choices)
        }
    }

    @Test fun mixedGroupSurvivesImageAndVideoViewerAndOnlyExplicitBackClearsIt() {
        val session = CatalogAttachmentSession()
        val group = listOf(attachment(AttachmentViewer.IMAGE), attachment(AttachmentViewer.VIDEO))
        var opens = 0
        session.openOrChoose(group) { opens++ }
        assertEquals(0, opens)
        for (item in group) {
            session.select(item) { opens++ }
            assertSame(group, session.choices)
            assertTrue(session.visible)
        }
        assertEquals(2, opens)
        session.dismiss()
        assertNull(session.choices)
    }

    @Test fun emptyGroupAndForeignSelectionCannotOpenAViewer() {
        val session = CatalogAttachmentSession()
        var opens = 0
        session.openOrChoose(emptyList()) { opens++ }
        session.select(attachment(AttachmentViewer.VIDEO)) { opens++ }
        assertEquals(0, opens)
        assertNull(session.choices)
    }

    private fun attachment(viewer: AttachmentViewer) =
        MediaAttachmentUi(
            MediaAttachmentSource("https://example.com/${viewer.name}", viewer.name, "https://example.com/thumb", null),
            null,
            viewer,
        )
}
