package com.solthariel.pawchan

import com.solthariel.pawchan.arhivach.ArchivePreviewState
import com.solthariel.pawchan.arhivach.toArchiveThreadPresentationUi
import com.solthariel.pawchan.domain.archive.ArchiveMedia
import com.solthariel.pawchan.domain.archive.ArchiveMediaDetails
import com.solthariel.pawchan.domain.archive.ArchivePost
import com.solthariel.pawchan.domain.archive.ArchivePostDetails
import com.solthariel.pawchan.domain.archive.ArchiveThread
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.media.AttachmentViewer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArchivePostPresentationTest {
    @Test
    fun archivePresentationKeepsPostOrderAndUsesOnlyProvidedThumbnails() {
        val media = ArchiveMedia("https://archive.example/image.jpg", null)
        val post = ArchivePost("77", "Аноним", "14.11.26 02:05:15", "Текст", listOf(media))
        val presentation = ArchiveThread(77, "Тест", listOf(post)).toArchiveThreadPresentationUi()

        assertEquals(1, presentation.posts.single().ordinal)
        assertEquals(AttachmentViewer.IMAGE, presentation.media.single().viewer)
        assertEquals(null, presentation.media.single().thumbnailUrl)
    }

    @Test
    fun archiveGalleryUsesSharedMediaGroupsInPostOrder() {
        val media =
            listOf("one.jpg", "two.mp4", "three.mp3", "one.jpg")
                .map { ArchiveMedia("https://archive.example/$it", null) }
        val post = ArchivePost("1", null, null, "", media)
        val gallery = ArchiveThread(1, "title", listOf(post)).toArchiveThreadPresentationUi().gallery

        assertEquals(listOf("one.jpg", "two.mp4", "one.jpg"), gallery.all.items.map { it.source.fileName })
        assertEquals(2, gallery.images.count)
        assertEquals(1, gallery.videos.count)
        assertEquals(
            gallery.all.keys.size,
            gallery.all.keys
                .distinct()
                .size,
        )
    }

    @Test
    fun mediaShowsSourceDimensionsAndRoundedSizeWithoutClaimingExactBytes() {
        val media =
            ArchiveMedia(
                "https://archive.example/image.jpg",
                null,
                ArchiveMediaDetails("672.6 КБ", MediaDimensions.create(1280, 960)),
            )
        val post = ArchivePost("1", null, null, "", listOf(media))
        val result = ArchiveThread(1, "title", listOf(post)).toArchiveThreadPresentationUi().media.single()
        assertEquals("JPEG 672.6 КБ", result.display.typeAndSize)
        assertEquals("1280×960", result.displayDimensions)
        assertNull(result.byteSize)
    }

    @Test
    fun archiveUsesPostSubjectOnlyAndSuppressesRepeatedBodyText() {
        val distinct =
            ArchivePost("1", null, null, "Текст", emptyList(), details = ArchivePostDetails(subject = "Тема"))
        val duplicate =
            ArchivePost("2", null, null, "Тема и текст", emptyList(), details = ArchivePostDetails(subject = "Тема"))
        val absent = ArchivePost("3", null, null, "Текст", emptyList())
        val posts =
            ArchiveThread(1, "Заголовок страницы — текст-превью", listOf(distinct, duplicate, absent))
                .toArchiveThreadPresentationUi()
                .posts

        assertEquals("Тема", posts[0].subject)
        assertNull(posts[1].subject)
        assertNull(posts[2].subject)
    }

    @Test
    fun archiveReferencesAndRepliesOnlyUseUniqueAvailablePostNumbers() {
        val op = ArchivePost("100", null, null, "OP", emptyList())
        val reference = PostLink.Reference(requireNotNull(PostId.parse(100)))
        val missing = PostLink.Reference(requireNotNull(PostId.parse(999)))
        val duplicate = PostLink.Reference(requireNotNull(PostId.parse(200)))
        val document =
            PostDocument(
                listOf(
                    PostTextRun(">>100", link = reference),
                    PostTextRun(" "),
                    PostTextRun(">>100", link = reference),
                    PostTextRun(" "),
                    PostTextRun(">>999", link = missing),
                    PostTextRun(" "),
                    PostTextRun(">>200", link = duplicate),
                ),
            )
        val reply =
            ArchivePost(
                "101",
                null,
                null,
                document.plainText,
                emptyList(),
                details = ArchivePostDetails(document = document),
            )
        val duplicateOne = ArchivePost("200", null, null, "one", emptyList())
        val duplicateTwo = ArchivePost("200", null, null, "two", emptyList())
        val presentation =
            ArchiveThread(55, "title", listOf(op, reply, duplicateOne, duplicateTwo))
                .toArchiveThreadPresentationUi()

        assertEquals(
            listOf("100", "101"),
            presentation.byNumber.keys
                .sorted()
                .map { it.toString() },
        )
        assertEquals(listOf("101"), presentation.replies.getValue(100).map { it.post.number })
        val runs = presentation.posts[1].document.runs
        assertEquals(2, runs.count { it.link == reference })
        assertTrue(runs.any { it.text == ">>999" && it.link == null })
        assertTrue(runs.any { it.text == ">>200" && it.link == null })
        assertNull(presentation.replies[200])
    }

    @Test
    fun archivePreviewStackClosesTheTopAndKeepsRepeatedTargetsSeparate() {
        val stack = ArchivePreviewState()
        stack.open(100, replies = false)
        stack.open(100, replies = false)
        assertEquals(2, stack.entries.size)
        assertTrue(stack.entries[0].id != stack.entries[1].id)
        stack.close()
        assertEquals(1, stack.entries.size)
        stack.clear()
        assertTrue(stack.entries.isEmpty())
    }
}
