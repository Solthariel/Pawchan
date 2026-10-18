package com.solthariel.pawchan

import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaViewerSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MediaViewerSelectionTest {
    @Test
    fun previousAndNextFollowAttachmentOrderWithoutWrapping() {
        val items = listOf(attachment("one"), attachment("two"), attachment("three"))
        var selected = ""
        val middle = MediaViewerSelection(items[1], items) { selected = it.id }.navigation()
        assertEquals(2, middle.position)
        assertEquals(3, middle.total)
        middle.previous?.invoke()
        assertEquals("one", selected)
        middle.next?.invoke()
        assertEquals("three", selected)
        assertNull(MediaViewerSelection(items[0], items) {}.navigation().previous)
        assertNull(MediaViewerSelection(items[2], items) {}.navigation().next)
    }

    @Test
    fun missingSelectionCannotSkipToAnUnrelatedFirstItem() {
        val missing = MediaViewerSelection(attachment("other"), listOf(attachment("one"))) {}.navigation()
        assertNull(missing.previous)
        assertNull(missing.next)
        assertEquals(1, missing.total)
    }

    private fun attachment(id: String): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource(id, "$id.png", "https://example.com/$id.png", null),
            null,
            AttachmentViewer.IMAGE,
        )
}
