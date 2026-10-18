package com.solthariel.pawchan

import com.solthariel.pawchan.backup.ClientBackup
import com.solthariel.pawchan.backup.decodeClientBackup
import com.solthariel.pawchan.backup.encodeClientBackup
import com.solthariel.pawchan.backup.readClientBackup
import com.solthariel.pawchan.data.drafts.WorkspaceTransfer
import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.ThreadMarks
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.navigation.boardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class ClientBackupTest {
    private fun address(): ThreadAddress {
        val board = requireNotNull(boardAddress("dvach", "b"))
        return ThreadAddress(board, requireNotNull(ThreadId.parse(42L)))
    }

    @Test fun roundTripIncludesFavoritesButNotVisitsOrTabs() {
        val thread = address()
        val state =
            WorkspaceState(
                tabs = listOf(OpenThread(thread, "private")),
                lastRoute = "private",
                marks = ThreadMarks(hidden = setOf(thread), seen = mapOf(thread to 45L)),
            )
        val source =
            ClientBackup(
                mapOf("auto_wifi_only" to "true"),
                listOf(
                    ThreadLibraryEntry(thread, "Тест", 100L, true),
                    ThreadLibraryEntry(thread, "History", 100L, false),
                ),
                state,
            )
        val restored = decodeClientBackup(encodeClientBackup(source))
        assertEquals(source.settings + ("favorite_boards_v2" to ""), restored.settings)
        assertEquals(1, restored.favorites.size)
        assertEquals(0L, restored.favorites.single().lastVisitedAtEpochMillis)
        assertEquals(setOf(thread), restored.bookmarks.marks.hidden)
        assertEquals(emptyList<OpenThread>(), restored.bookmarks.tabs)
        assertEquals(emptyMap<ThreadAddress, Long>(), restored.bookmarks.marks.seen)
        assertNull(restored.bookmarks.lastRoute)
    }

    @Test fun bookmarkMergePreservesExistingTabsAndHiddenThreads() {
        val current = WorkspaceState(tabs = listOf(OpenThread(address(), "existing")))
        val incoming = WorkspaceState(marks = ThreadMarks(hidden = setOf(address())))
        val merged = WorkspaceTransfer.merge(current, incoming)
        assertEquals(current.tabs, merged.tabs)
        assertEquals(incoming.marks.hidden, merged.marks.hidden)
    }

    @Test(expected = IllegalArgumentException::class)
    fun unknownBackupVersionRejected() {
        decodeClientBackup("format=pawchan-client\nversion=999\nfavorites=0\n".toByteArray())
    }

    @Test fun boundedReaderPreservesUtf8Bytes() {
        val bytes = "тест".toByteArray(Charsets.UTF_8)
        assertEquals(bytes.toList(), readClientBackup(ByteArrayInputStream(bytes)).toList())
    }
}
