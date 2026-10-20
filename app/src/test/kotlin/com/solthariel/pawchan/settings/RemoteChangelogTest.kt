package com.solthariel.pawchan.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteChangelogTest {
    @Test fun laterPublishedVersionsAreVisibleBeforeInstallingThem() {
        assertEquals(listOf(2, 1), decodeRemoteChangelog(historyBody(1, 2)).map { it.code })
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateCodesAreRejected() {
        decodeRemoteChangelog(historyBody(1, 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun malformedNotesAreRejected() {
        decodeRemoteChangelog(historyBody(1).replace("• Исправление", ""))
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownSchemaIsRejected() {
        decodeRemoteChangelog(historyBody(1).replace("\"schema\":1", "\"schema\":2"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun oversizedHistoryIsRejected() {
        decodeRemoteChangelog(" ".repeat(MAX_HISTORY_BYTES + 1))
    }

    @Test fun usesTheUpdateFeedDirectoryAndRequiresHttps() {
        assertEquals("https://example.org/feed/history-v1.json", historyUrl("https://example.org/feed/data-v1.json"))
        assertNull(historyUrl("http://example.org/feed/data-v1.json"))
        assertNull(historyUrl(""))
    }
}

internal fun historyBody(vararg codes: Int): String =
    """{"schema":1,"releases":[${codes.joinToString(",") {
        """{"code":$it,"name":"0.1.$it","date":"21.10.2026","notes":"Исправления\n• Исправление"}"""
    }}]}"""
