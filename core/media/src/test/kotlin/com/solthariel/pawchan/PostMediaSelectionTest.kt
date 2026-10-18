package com.solthariel.pawchan

import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MediaAttachmentDisplay
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaViewerNavigation
import com.solthariel.pawchan.media.mediaToolbarSubtitle
import com.solthariel.pawchan.media.postMediaSelection
import com.solthariel.pawchan.media.video.videoTimeLabel
import com.solthariel.pawchan.media.viewerItems
import com.solthariel.pawchan.media.withMediaOwner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PostMediaSelectionTest {
    @Test
    fun onlySelectedPostsAttachmentsAreCountedAndPaged() {
        val first = listOf(attachment("a"), attachment("b")).withMediaOwner(101L)
        val second = listOf(attachment("c")).withMediaOwner(102L)
        val groups = mapOf(101L to first, 102L to second)
        var selected = ""
        val navigation = postMediaSelection(first[0], groups) { selected = it.id }.navigation()
        assertEquals(1, navigation.position)
        assertEquals(2, navigation.total)
        navigation.next?.invoke()
        assertEquals("b", selected)
        assertNull(postMediaSelection(first[1], groups) {}.navigation().next)
        assertEquals(1, postMediaSelection(second[0], groups) {}.navigation().total)
    }

    @Test
    fun missingOwnerNeverFallsBackToWholeThread() {
        val navigation =
            postMediaSelection(attachment("unknown"), mapOf(10L to listOf(attachment("other")))) {}
                .navigation()
        assertEquals(1, navigation.total)
        assertNull(navigation.next)
        assertNull(navigation.previous)
    }

    @Test
    fun nonViewableFilesAreExcludedFromPostCounter() {
        val items = listOf(attachment("a"), attachment("file").copy(viewer = AttachmentViewer.NONE))
        assertEquals(listOf("a"), items.viewerItems().map { it.id })
    }

    @Test
    fun toolbarUsesPostPositionAndPreparedFileMetadata() {
        val item = attachment("a").copy(display = MediaAttachmentDisplay("WEBP", "262 kB", "1332×1920"))
        assertEquals("1/4, 1332×1920, 262 kB", mediaToolbarSubtitle(item, MediaViewerNavigation(total = 4)))
        assertEquals("1/1", mediaToolbarSubtitle(attachment("a"), MediaViewerNavigation()))
    }

    @Test
    fun videoTimeShowsPositionAndDurationInMinutesAndSeconds() {
        assertEquals("0:00", videoTimeLabel(-1L))
        assertEquals("1:05", videoTimeLabel(65_000L))
        assertEquals("62:03", videoTimeLabel(3_723_000L))
    }

    private fun attachment(id: String): MediaAttachmentUi =
        MediaAttachmentUi(
            MediaAttachmentSource(id, "$id.png", "https://example.com/$id.png", null),
            null,
            AttachmentViewer.IMAGE,
        )
}
