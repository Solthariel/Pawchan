package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.GalleryFilter
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaGallerySession
import com.solthariel.pawchan.media.MediaViewerSelection
import com.solthariel.pawchan.media.toMediaGalleryContent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalleryViewerFilterTest {
    @Test fun viewerNavigatesOnlyTheCurrentFilterInGalleryOrder() {
        val image = item("image", MediaKind.IMAGE)
        val firstVideo = item("video1", MediaKind.VIDEO)
        val gif = item("gif", MediaKind.ANIMATION)
        val secondVideo = item("video2", MediaKind.VIDEO)
        val gallery = listOf(image, firstVideo, gif, secondVideo).toMediaGalleryContent()
        val session = MediaGallerySession()
        for ((filter, expected) in listOf(
            GalleryFilter.ALL to listOf(image, firstVideo, gif, secondVideo),
            GalleryFilter.IMAGES to listOf(image),
            GalleryFilter.GIF to listOf(gif),
            GalleryFilter.VIDEO to listOf(firstVideo, secondVideo),
        )) {
            session.filter = filter
            assertEquals(expected, session.viewerItems(gallery, false))
        }
        session.filter = GalleryFilter.VIDEO
        val navigation = MediaViewerSelection(firstVideo, session.viewerItems(gallery, false)) {}.navigation()
        assertNull(navigation.previousItem)
        assertEquals(secondVideo, navigation.nextItem)
        assertEquals(2, navigation.total)
    }

    @Test fun viewerRespectsHiddenStickersAndEmptyFilter() {
        val photo = item("photo", MediaKind.IMAGE)
        val sticker = item("sticker", MediaKind.IMAGE).copy(isSticker = true)
        val gallery = listOf(photo, sticker).toMediaGalleryContent()
        val session = MediaGallerySession().apply { filter = GalleryFilter.IMAGES }
        assertEquals(listOf(photo), session.viewerItems(gallery, true))
        assertEquals(listOf(photo, sticker), session.viewerItems(gallery, false))
        session.filter = GalleryFilter.VIDEO
        assertEquals(emptyList<MediaAttachmentUi>(), session.viewerItems(gallery, true))
    }

    private fun item(
        id: String,
        kind: MediaKind,
    ): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource(id, "$id.dat", "https://example.org/$id", null),
            null,
            if (kind == MediaKind.VIDEO) AttachmentViewer.VIDEO else AttachmentViewer.IMAGE,
            kind,
        )
}
