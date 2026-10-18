package com.solthariel.pawchan

import com.solthariel.pawchan.media.AttachmentViewer
import com.solthariel.pawchan.media.MEDIA_TOOLBAR_ALPHA
import com.solthariel.pawchan.media.MediaAttachmentSource
import com.solthariel.pawchan.media.MediaAttachmentUi
import com.solthariel.pawchan.media.MediaViewerSelection
import com.solthariel.pawchan.media.postMediaSelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ThreadMediaScopeTest {
    @Test
    fun threadPagesAcrossPostsAndIncludesVideoWhileCatalogStaysWithinPost() {
        val first = listOf(attachment("a", 101), attachment("b", 101))
        val video = attachment("c", 102).copy(viewer = AttachmentViewer.VIDEO)
        val all = first + video
        var selected = ""
        val thread = MediaViewerSelection(first.last(), all) { selected = it.id }.navigation()
        assertEquals(2, thread.position)
        assertEquals(3, thread.total)
        thread.next?.invoke()
        assertEquals("c", selected)
        val catalog = postMediaSelection(first.last(), mapOf(101L to first)) {}.navigation()
        assertEquals(2, catalog.total)
        assertNull(catalog.next)
        assertEquals(3, MediaViewerSelection(video, all) {}.navigation().position)
    }

    @Test
    fun repeatedUrlInAnotherPostHasItsOwnPosition() {
        val first = attachment("same", 101)
        val second = attachment("same", 102)
        assertEquals(2, MediaViewerSelection(second, listOf(first, second)) {}.navigation().position)
    }

    @Test
    fun mediaUsesMediaOverlayOpacity() {
        assertEquals(170f / 255f, MEDIA_TOOLBAR_ALPHA, 0.00001f)
    }

    private fun attachment(
        id: String,
        owner: Long,
    ) = MediaAttachmentUi(
        MediaAttachmentSource(id, "$id.png", "https://example.com/$id.png", null, owner),
        null,
        AttachmentViewer.IMAGE,
    )
}
