package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadPayloadDecoderTest {
    private val decoder =
        ThreadPayloadDecoder(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )

    @Test
    fun decodesPostsAndAttachments() {
        val snapshot = requireNotNull(decoder.decode(address(), fixture("thread-success.json")))

        assertEquals("Тестовый тред", snapshot.subject)
        assertEquals(2, snapshot.uniquePosters)
        assertTrue(snapshot.flags.pinned)
        assertEquals(listOf(42L, 43L), snapshot.posts.map { it.address.post.value })
        assertTrue(
            snapshot.posts
                .last()
                .attachments
                .isEmpty(),
        )
        val attachment =
            snapshot.posts
                .first()
                .attachments
                .single()
        assertEquals("cat.jpg", attachment.fileName)
        assertEquals(MediaKind.IMAGE, attachment.kind)
        assertEquals(
            "Аноним",
            snapshot.posts
                .first()
                .content
                .metadata
                .name,
        )
        assertEquals(
            "01/01/26 Пнд 12:00:00",
            snapshot.posts
                .first()
                .content
                .metadata
                .date,
        )
        assertTrue(
            snapshot.posts
                .last()
                .content
                .metadata
                .sage,
        )
    }

    @Test
    fun decodesTagTimestampTripAndBan() {
        val snapshot = requireNotNull(decoder.decode(address(), fixture("thread-success.json")))
        val first =
            snapshot.posts
                .first()
                .content.details
        val last =
            snapshot.posts
                .last()
                .content.details
        assertEquals("news", snapshot.tag)
        assertEquals(1767268800L, first.timestamp)
        assertEquals("!!abc", first.tripcode)
        assertTrue(last.banned)
        // sage — флаг метаданных, а не e-mail.
        assertNull(last.email)
    }

    @Test
    fun rejectsResponseForAnotherThread() {
        val wrongAddress =
            ThreadAddress(
                board = address().board,
                thread = requireNotNull(ThreadId.parse(99L)),
            )

        assertNull(decoder.decode(wrongAddress, fixture("thread-success.json")))
    }

    @Test
    fun skipsMalformedPostsAndAttachments() {
        val snapshot = requireNotNull(decoder.decode(address(), fixture("thread-partially-malformed.json")))

        assertEquals(listOf(42L, 44L), snapshot.posts.map { it.address.post.value })
        assertTrue(
            snapshot.posts
                .first()
                .attachments
                .isEmpty(),
        )
    }

    private fun address(): ThreadAddress =
        ThreadAddress(
            board =
                BoardAddress(
                    site = requireNotNull(SiteId.parse("dvach")),
                    board = requireNotNull(BoardId.parse("b")),
                ),
            thread = requireNotNull(ThreadId.parse(42L)),
        )

    private fun fixture(name: String): String = requireNotNull(javaClass.getResource("/fixtures/$name")).readText()
}
