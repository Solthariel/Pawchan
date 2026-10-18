package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentDisplay
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.PostMediaDisplay
import com.solthariel.pawchan.media.toMediaGalleryContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PostMediaDisplayTest {
    @Test fun switchingUsesPreparedListsWithoutChangingTheSource() {
        val files = listOf(file("sticker", true), file("photo", false))
        val display = PostMediaDisplay(files)
        assertSame(files, display.visible(false))
        assertEquals(listOf(files[1]), display.visible(true))
        assertSame(display.withoutStickers, display.visible(true))
        assertSame(display.rowsWithoutStickers, display.visibleRows(true))
        assertEquals(listOf(files[0]), display.stickers)
        assertEquals(2, files.size)
    }

    @Test fun preparedGalleryPreservesStableKeysAndFiltersAllGroups() {
        val gallery = listOf(file("sticker", true), file("photo", false)).toMediaGalleryContent()
        val hidden = requireNotNull(gallery.withoutStickers)
        assertEquals(2, gallery.all.count)
        assertEquals(1, hidden.all.count)
        assertEquals(listOf("photo"), hidden.images.items.map { it.id })
        assertEquals(listOf(gallery.all.keys[1]), hidden.all.keys)
    }

    private fun file(
        id: String,
        sticker: Boolean,
    ): MediaAttachmentUi =
        MediaAttachmentUi(
            source = MediaAttachmentSource(id, "$id.png", "https://2ch.hk/$id.png", null, ownerPostId = 42),
            byteSize = null,
            viewer = AttachmentViewer.IMAGE,
            kind = MediaKind.IMAGE,
            isSticker = sticker,
            display = MediaAttachmentDisplay("PNG", null, null),
        )
}
