package com.solthariel.pawchan.backup

import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class BackupArchiveTest {
    @Test fun moreThanFiveHundredHistoryRecordsSurviveRoundTrip() {
        val entries = (1L..750L).map { entry(it, false, it) }
        val backup = ClientBackup(emptyMap(), entries, WorkspaceState(), setOf(BackupSection.HISTORY))
        assertEquals(entries, decodeClientBackup(encodeClientBackup(backup)).favorites)
    }

    @Test fun allSectionsHaveIndependentPayloadsAndMetadata() {
        val metadata = BackupMetadata(123L, "0.1.0")
        val source =
            ClientBackup(
                mapOf("theme" to "SYSTEM", "favorite_boards_v2" to "b", "app_content_filters_v1" to ""),
                listOf(entry(42L, true, 200L)),
                WorkspaceState(),
                BackupSection.entries.toSet(),
                mapOf("post:dvach:b:42:own" to "43"),
                metadata,
            )
        val restored = decodeClientBackup(encodeClientBackup(source))
        assertEquals(source, restored)
        assertEquals(0, backupCounts(restored)[BackupSection.FILTERS])
    }

    @Test fun unselectedOwnMarkersDoNotLeakIntoArchive() {
        val source =
            ClientBackup(
                emptyMap(),
                emptyList(),
                WorkspaceState(),
                setOf(BackupSection.HISTORY),
                mapOf("post:dvach:b:42:own" to "43"),
            )
        assertTrue(decodeClientBackup(encodeClientBackup(source)).ownMarkers.isEmpty())
    }

    @Test(expected = IllegalArgumentException::class)
    fun pathTraversalEntriesAreRejectedWithoutExtraction() {
        decodeClientBackup(zip("../settings.json", "{}"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun duplicateThreadAddressesAreRejected() {
        val record = "{\"site\":\"dvach\",\"board\":\"b\",\"thread\":42,\"visited\":1}"
        decodeThreads("[$record,$record]", true)
    }

    @Test(expected = IllegalArgumentException::class)
    fun emptyExportSelectionIsRejected() {
        encodeClientBackup(ClientBackup(emptyMap(), emptyList(), WorkspaceState(), emptySet()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun truncatedZipDirectoryIsRejected() {
        val source = ClientBackup(emptyMap(), emptyList(), WorkspaceState(), setOf(BackupSection.HISTORY))
        val archive = encodeClientBackup(source)
        decodeClientBackup(archive.copyOf(archive.size - 1))
    }

    private fun entry(
        id: Long,
        favorite: Boolean,
        visited: Long,
    ): ThreadLibraryEntry {
        val board = BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b")))
        return ThreadLibraryEntry(
            ThreadAddress(board, requireNotNull(ThreadId.parse(id))),
            "Тред $id",
            visited,
            favorite,
        )
    }

    private fun zip(
        name: String,
        text: String,
    ): ByteArray =
        ByteArrayOutputStream()
            .also { output ->
                ZipOutputStream(output).use { zip ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(text.toByteArray())
                    zip.closeEntry()
                }
            }.toByteArray()
}
