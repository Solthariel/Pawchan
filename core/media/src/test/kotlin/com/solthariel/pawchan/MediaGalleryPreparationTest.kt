package com.solthariel.pawchan

import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MEDIA_ATTACHMENT_COLUMNS
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.toMediaGalleryContent
import com.solthariel.pawchan.media.withMediaOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test

class MediaGalleryPreparationTest {
    @Test fun sameFileInSeveralPostsGetsUniqueStableGalleryKeys() {
        // 2ch отдаёт md5 как id: один и тот же файл в разных постах (и даже дважды в одном).
        val repost =
            listOf(item("md5")).withMediaOwner(1L) +
                listOf(item("md5"), item("md5")).withMediaOwner(2L) +
                listOf(item("other")).withMediaOwner(3L)
        val gallery = repost.toMediaGalleryContent()
        val keys = gallery.all.keys
        assertEquals(listOf("1/md5", "2/md5", "2/md5#1", "3/other"), keys)
        assertEquals(keys.size, keys.toSet().size)
        assertEquals(keys, gallery.images.keys)
        val rowKeys =
            gallery.all.rows.indices
                .map { gallery.all.keyAt(it * MEDIA_ATTACHMENT_COLUMNS) }
        assertEquals(rowKeys.size, rowKeys.toSet().size)
    }

    @Test fun emptyGalleryHasNoRowsOrStaleCounts() {
        val gallery = emptyList<MediaAttachmentUi>().toMediaGalleryContent()
        for (group in listOf(gallery.all, gallery.images, gallery.animations, gallery.videos)) {
            assertEquals(0, group.count)
            assertEquals(emptyList<MediaAttachmentUi>(), group.items)
            assertEquals(emptyList<List<MediaAttachmentUi>>(), group.rows)
        }
    }

    @Test fun mixedKindsKeepOriginalOrderAndIndependentFilters() {
        val source = listOf(item("gif", MediaKind.ANIMATION), item("png"), item("mp4", MediaKind.VIDEO))
        val gallery = source.toMediaGalleryContent()
        assertEquals(source, gallery.all.items)
        assertEquals(listOf("png"), gallery.images.items.map { it.id })
        assertEquals(listOf("gif"), gallery.animations.items.map { it.id })
        assertEquals(listOf("mp4"), gallery.videos.items.map { it.id })
        val counts = listOf(gallery.all, gallery.images, gallery.animations, gallery.videos).map { it.count }
        assertEquals(listOf(3, 1, 1, 1), counts)
    }

    @Test fun unsupportedViewerDoesNotEnterGalleryOrInflateCounters() {
        val unsupported = item("file", MediaKind.FILE).copy(viewer = AttachmentViewer.NONE)
        val gallery = listOf(unsupported, item("image")).toMediaGalleryContent()
        assertEquals(1, gallery.all.count)
        assertEquals(
            "image",
            gallery.all.items
                .single()
                .id,
        )
        assertFalse(
            gallery.all.rows
                .flatten()
                .contains(unsupported),
        )
    }

    @Test fun everyPreparedRowUsesTheMediaOwnedColumnCount() {
        for (size in 1..13) {
            val source = List(size) { item("$it") }
            val rows = source.toMediaGalleryContent().all.rows
            assertEquals(source, rows.flatten())
            assertEquals((size + MEDIA_ATTACHMENT_COLUMNS - 1) / MEDIA_ATTACHMENT_COLUMNS, rows.size)
            assertEquals(source.chunked(4), rows)
        }
    }

    @Test fun galleryPreservesPostOwnershipEvenWhenIdsAndUrlsMatch() {
        val first = listOf(item("same")).withMediaOwner(101)
        val second = listOf(item("same")).withMediaOwner(102)
        val items = (first + second).toMediaGalleryContent().all.items
        assertEquals(2, items.size)
        assertEquals(listOf(101L, 102L), items.map { it.source.ownerPostId })
        assertSame(first.single(), items.first())
        assertSame(second.single(), items.last())
    }

    @Test fun changingTheInputListDoesNotModifyAPreparedGallery() {
        val source = mutableListOf(item("first"), item("second"))
        val gallery = source.toMediaGalleryContent()
        source.clear()
        assertEquals(listOf("first", "second"), gallery.all.items.map { it.id })
        assertEquals(gallery.all.items, gallery.all.rows.flatten())
    }

    @Test fun galleryPreparationDoesNotResetDisplayOrOwnership() {
        val media = listOf(item("photo")).withMediaOwner(42).single()
        val result =
            listOf(media)
                .toMediaGalleryContent()
                .images.items
                .single()
        assertSame(media, result)
        assertSame(media.display, result.display)
        assertEquals(42L, result.source.ownerPostId)
    }

    private fun item(
        id: String,
        kind: MediaKind = MediaKind.IMAGE,
    ): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource(id, "$id.png", "https://example.invalid/$id.png", null),
            1_024,
            if (kind == MediaKind.VIDEO) AttachmentViewer.VIDEO else AttachmentViewer.IMAGE,
            kind,
        )
}
