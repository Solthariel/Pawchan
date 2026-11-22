package com.solthariel.pawchan.media

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.FileNotFoundException

class MediaDocumentCreationTest {
    @Test fun keepsCreatedDocumentsAndSkipsUnavailableFiles() {
        val discarded = mutableListOf<String>()
        val result =
            createMediaDocuments(
                listOf("one.jpg", "unavailable.jpg", "two.webm"),
                create = { file -> if (file == "unavailable.jpg") throw FileNotFoundException() else "uri:$file" },
                discard = discarded::add,
            )
        assertEquals(listOf("one.jpg" to "uri:one.jpg", "two.webm" to "uri:two.webm"), result)
        assertEquals(emptyList<String>(), discarded)
    }

    @Test fun revokedPermissionCleansUpCreatedDocumentsAndIsNotHidden() {
        val discarded = mutableListOf<String>()
        assertThrows(SecurityException::class.java) {
            createMediaDocuments(
                listOf("first.jpg", "second.jpg"),
                create = { file -> if (file == "second.jpg") throw SecurityException("revoked") else "uri:$file" },
                discard = discarded::add,
            )
        }
        assertEquals(listOf("uri:first.jpg"), discarded)
    }

    @Test fun mimeTypeUsesTheExtensionCaseInsensitively() {
        val types = mapOf("jpg" to "image/jpeg", "webm" to "video/webm")
        assertEquals("image/jpeg", mediaMimeTypeForName("portrait.JPG", types::get))
        assertEquals("video/webm", mediaMimeTypeForName("video.webm", types::get))
        assertEquals("application/octet-stream", mediaMimeTypeForName("file.unknown", types::get))
        assertEquals("application/octet-stream", mediaMimeTypeForName("no-extension", types::get))
        assertNull(types["JPG"])
    }
}
