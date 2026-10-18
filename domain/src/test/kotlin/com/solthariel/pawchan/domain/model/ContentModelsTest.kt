package com.solthariel.pawchan.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentModelsTest {
    @Test
    fun addressesKeepTheirHierarchyExplicit() {
        val board = boardAddress()
        val thread = ThreadAddress(board, requireNotNull(ThreadId.parse(42L)))
        val post = PostAddress(thread, requireNotNull(PostId.parse(43L)))

        assertEquals("dvach", post.site.value)
        assertEquals("b", post.thread.board.board.value)
        assertEquals(42L, post.thread.thread.value)
        assertEquals(43L, post.post.value)
    }

    @Test
    fun statisticsRejectImpossibleCounts() {
        assertEquals(4, ThreadStatistics.create(replies = 10, files = 4, images = 3)?.files)
        assertNull(ThreadStatistics.create(replies = -1, files = 0, images = 0))
        assertNull(ThreadStatistics.create(replies = 1, files = 1, images = 2))
    }

    @Test
    fun mediaDimensionsRequireTwoPositiveSides() {
        val dimensions = requireNotNull(MediaDimensions.create(516, 595))
        assertEquals(516, dimensions.width)
        assertEquals(595, dimensions.height)
        assertNull(MediaDimensions.create(null, 595))
        assertNull(MediaDimensions.create(516, null))
        assertNull(MediaDimensions.create(0, 595))
        assertNull(MediaDimensions.create(516, -1))
    }

    @Test
    fun snapshotsDefensivelyCopyMutableInputs() {
        val threads = mutableListOf<CatalogThread>()
        val snapshot =
            CatalogSnapshot(
                board = BoardDescriptor(boardAddress(), "Бред", worksafe = false),
                threads = threads,
            )

        threads += catalogThread()
        assertEquals(0, snapshot.threads.size)
    }

    private fun catalogThread(): CatalogThread {
        val board = boardAddress()
        return CatalogThread(
            address = ThreadAddress(board, requireNotNull(ThreadId.parse(42L))),
            subject = null,
            content =
                PostContent(
                    document = PostDocument.plain(""),
                    metadata = PostMetadata(name = null, date = null, sage = false),
                ),
            attachments = emptyList(),
            statistics = requireNotNull(ThreadStatistics.create(0, 0, 0)),
            flags = ThreadFlags(pinned = false, closed = false, archived = false),
        )
    }

    private fun boardAddress(): BoardAddress =
        BoardAddress(
            site = requireNotNull(SiteId.parse("dvach")),
            board = requireNotNull(BoardId.parse("b")),
        )
}
