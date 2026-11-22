package com.solthariel.pawchan

import com.solthariel.pawchan.arhivach.ArchiveListMediaChooser
import com.solthariel.pawchan.arhivach.ArchiveListMediaState
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchiveListMediaChooserTest {
    @Test
    fun multiFilePreviewShowsChooserBeforeOpeningSelectedMedia() {
        val chooser = ArchiveListMediaChooser()
        val items = List(4) { attachment(it) }
        var opened: ArchiveListMediaState? = null

        chooser.open(items) { opened = it }

        assertTrue(chooser.visible)
        assertSame(items, chooser.choices)
        assertNull(opened)

        chooser.select(items[2]) { opened = it }

        assertTrue(chooser.visible)
        assertSame(items[2], opened?.attachment)
        assertSame(items, opened?.items)
    }

    @Test
    fun singleFilePreviewOpensDirectlyWithoutChooser() {
        val chooser = ArchiveListMediaChooser()
        val item = attachment(1)
        var opened: ArchiveListMediaState? = null

        chooser.open(listOf(item)) { opened = it }

        assertFalse(chooser.visible)
        assertNull(chooser.choices)
        assertSame(item, opened?.attachment)
        assertSame(item, opened?.items?.single())
    }

    @Test
    fun chooserDismissalClearsThePendingGroup() {
        val chooser = ArchiveListMediaChooser()
        chooser.open(List(4) { attachment(it) }) {}

        chooser.dismiss()

        assertFalse(chooser.visible)
        assertNull(chooser.choices)
    }

    private fun attachment(index: Int) =
        MediaAttachmentUi(
            source =
                MediaAttachmentSource(
                    id = "file-$index",
                    fileName = "$index.jpg",
                    originalUrl = "https://example.com/$index.jpg",
                    thumbnailUrl = "https://example.com/$index-small.jpg",
                ),
            byteSize = null,
            viewer = AttachmentViewer.IMAGE,
        )
}
