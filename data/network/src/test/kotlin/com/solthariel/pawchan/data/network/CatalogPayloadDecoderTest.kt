package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.model.MediaKind
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogPayloadDecoderTest {
    private val decoder =
        CatalogPayloadDecoder(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )

    @Test
    fun decodesCatalogFixtureIntoDomainModels() {
        val catalog = requireNotNull(decoder.decode(fixture("catalog-success.json")))
        val thread = catalog.threads.single()

        assertEquals("b", catalog.board.address.board.value)
        assertEquals("Бред", catalog.board.title)
        assertEquals(136, catalog.boardSpeed)
        assertEquals(328868282L, thread.address.thread.value)
        assertEquals(">Тестовый тред", thread.subject)
        assertTrue(thread.flags.pinned)
        assertFalse(thread.flags.closed)
        assertTrue(thread.flags.endless)
        assertEquals(12, thread.statistics.replies)
        assertEquals(2, thread.statistics.files)
        assertEquals(1, thread.statistics.images)
        assertEquals(543L * 1024L, thread.statistics.knownMediaBytes)
        assertTrue(thread.statistics.mediaBytesComplete)
        val firstAttachmentSize = thread.attachments[0].byteSize?.value
        assertEquals(31L * 1024L, firstAttachmentSize)
        assertEquals(
            "https://2ch.su/b/src/328868282/17686951847150.jpg",
            thread.attachments[0].remoteUrl.value,
        )
        assertEquals(516, thread.attachments[0].dimensions?.width)
        assertEquals(595, thread.attachments[0].dimensions?.height)
        assertNull(thread.attachments[1].dimensions)
        assertEquals(MediaKind.VIDEO, thread.attachments.last().kind)
    }

    @Test
    fun suppliesOnlyDocumentedDefaultsForOptionalFields() {
        val catalog = requireNotNull(decoder.decode(fixture("catalog-minimal.json")))
        val thread = catalog.threads.single()

        assertEquals(0, catalog.boardSpeed)
        assertNull(thread.subject)
        assertEquals("", thread.content.document.plainText)
        assertEquals(0, thread.statistics.replies)
        assertTrue(thread.attachments.isEmpty())
        assertFalse(thread.flags.pinned)
    }

    @Test
    fun acceptsSpeedAliasAtRoot() {
        val payload =
            fixture("catalog-success.json")
                .replace("\"board_speed\": 136,", "\"speed\": 1367,")

        assertEquals(1367, requireNotNull(decoder.decode(payload)).boardSpeed)
    }

    @Test
    fun acceptsStringSpeedInsideBoardObject() {
        val payload =
            fixture("catalog-success.json")
                .replace(
                    "\"name\": \"Бред\",",
                    "\"name\": \"Бред\", \"speed\": \"1367\",",
                ).replace("  \"board_speed\": 136,\n", "")

        assertEquals(1367, requireNotNull(decoder.decode(payload)).boardSpeed)
    }

    @Test
    fun keepsAttachmentButDropsInvalidDimensions() {
        val payload = fixture("catalog-success.json").replace("\"width\": 516", "\"width\": 0")
        val attachment =
            requireNotNull(decoder.decode(payload))
                .threads
                .single()
                .attachments
                .first()

        assertEquals("picture.jpeg", attachment.fileName)
        assertNull(attachment.dimensions)
    }

    @Test
    fun preservesNavigationSubjectEvenWhenItRepeatsComment() {
        val thread =
            requireNotNull(decoder.decode(fixture("catalog-subject-duplicate.json")))
                .threads
                .single()

        assertEquals("Sample subject", thread.subject)
        assertEquals(
            "Sample subject\nGeneric test message\n",
            thread.content.document.plainText,
        )
    }

    @Test
    fun skipsMalformedThreadsAndAttachments() {
        val catalog = requireNotNull(decoder.decode(fixture("catalog-partially-malformed.json")))

        assertEquals(listOf(42L), catalog.threads.map { it.address.thread.value })
        assertTrue(
            catalog.threads
                .single()
                .attachments
                .isEmpty(),
        )
    }

    private fun fixture(name: String): String =
        requireNotNull(javaClass.getResource("/fixtures/$name"))
            .readText()
            .replace("\r\n", "\n")
}
