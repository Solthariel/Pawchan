package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaMenuItem
import com.solthariel.pawchan.media.MediaMenuPlace
import com.solthariel.pawchan.media.MediaMenuRequest
import com.solthariel.pawchan.media.MediaViewerMenu
import com.solthariel.pawchan.media.mediaMenuItems
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaMenuItemsTest {
    private val common = listOf(MediaMenuItem.DOWNLOAD, MediaMenuItem.COPY_LINK)

    @Test fun thumbnailMenuIsSearchPlusCommonBlockEvenInCatalog() {
        val request = MediaMenuRequest(image(), MediaMenuPlace.THUMBNAIL)
        assertEquals(listOf(MediaMenuItem.SEARCH) + common, mediaMenuItems(request, true, true, canOpenThread = true))
    }

    @Test fun galleryMenuStartsWithGoToPostWhereScreenSupportsIt() {
        val request = MediaMenuRequest(image(), MediaMenuPlace.GALLERY)
        assertEquals(
            listOf(MediaMenuItem.GO_TO_POST, MediaMenuItem.SEARCH) + common,
            mediaMenuItems(request, canCopyImage = true, canGoToPost = true),
        )
        assertEquals(false, MediaMenuItem.GO_TO_POST in mediaMenuItems(request, true, canGoToPost = false))
    }

    @Test fun viewerMenuAddsOpenFileItemsAndRefreshOnlyWhenChromeIsHidden() {
        val shown = MediaMenuRequest(image(), MediaMenuPlace.VIEWER, MediaViewerMenu())
        assertEquals(
            listOf(MediaMenuItem.SEARCH, MediaMenuItem.COPY_IMAGE, MediaMenuItem.OPEN_THREAD) + common,
            mediaMenuItems(shown, canCopyImage = true, canGoToPost = false, canOpenThread = true),
        )
        val hidden = shown.copy(viewer = MediaViewerMenu(onRefresh = {}))
        assertEquals(
            listOf(MediaMenuItem.REFRESH) + common,
            mediaMenuItems(hidden, canCopyImage = true, canGoToPost = false).takeLast(3),
        )
    }

    @Test fun videoViewerMenuEndsWithDecoderToggle() {
        val video = image().copy(viewer = AttachmentViewer.VIDEO, kind = MediaKind.VIDEO)
        val request = MediaMenuRequest(video, MediaMenuPlace.VIEWER, MediaViewerMenu(softwareDecoder = false))
        val items = mediaMenuItems(request, canCopyImage = false, canGoToPost = true)
        assertEquals(MediaMenuItem.SOFTWARE_DECODER, items.last())
        assertEquals(false, MediaMenuItem.COPY_IMAGE in items)
        assertEquals(true, MediaMenuItem.GO_TO_POST in items)
    }

    @Test fun viewerMenuStartsWithMetadataWhenAvailable() {
        val request = MediaMenuRequest(image(), MediaMenuPlace.VIEWER, MediaViewerMenu(metadata = { null }))
        assertEquals(MediaMenuItem.METADATA, mediaMenuItems(request, canCopyImage = true, canGoToPost = true).first())
    }

    private fun image(): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource("a", "a.png", "https://example.invalid/a.png", "https://example.invalid/a_s.png"),
            1_024,
            AttachmentViewer.IMAGE,
            MediaKind.IMAGE,
        )
}
