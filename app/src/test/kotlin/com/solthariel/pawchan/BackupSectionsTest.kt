package com.solthariel.pawchan

import com.solthariel.pawchan.backup.BackupSection
import com.solthariel.pawchan.backup.ClientBackup
import com.solthariel.pawchan.backup.decodeClientBackup
import com.solthariel.pawchan.backup.encodeClientBackup
import com.solthariel.pawchan.backup.selectedBackup
import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.workspace.ThreadMarks
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import com.solthariel.pawchan.navigation.boardAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupSectionsTest {
    private val address = ThreadAddress(requireNotNull(boardAddress("dvach", "b")), requireNotNull(ThreadId.parse(42L)))
    private val entry = ThreadLibraryEntry(address, "thread", 123L, true)

    @Test fun historyOnlyDoesNotRestoreFavoritesOrSettings() {
        val selected =
            selectedBackup(
                mapOf("auto_wifi_only" to "true"),
                listOf(entry),
                WorkspaceState(),
                setOf(BackupSection.HISTORY),
            )
        val restored = decodeClientBackup(encodeClientBackup(selected))
        assertTrue(restored.settings.isEmpty())
        assertFalse(restored.favorites.single().favorite)
        assertEquals(123L, restored.favorites.single().lastVisitedAtEpochMillis)
    }

    @Test fun filtersAreIndependentOfGeneralSettingsAndFavoriteBoards() {
        val values = mapOf("app_content_filters_v1" to "filters", "favorite_boards_v2" to "b", "theme" to "dark")
        val selected = selectedBackup(values, emptyList(), WorkspaceState(), setOf(BackupSection.FILTERS))
        assertEquals(mapOf("app_content_filters_v1" to "filters"), selected.settings)
    }

    @Test fun unselectedHiddenThreadsAreNotIncludedInSerializedCopy() {
        val state = WorkspaceState(marks = ThreadMarks(hidden = setOf(address)))
        val selected = ClientBackup(emptyMap(), emptyList(), state, setOf(BackupSection.SETTINGS))
        val restored = decodeClientBackup(encodeClientBackup(selected))
        assertTrue(
            restored.bookmarks.marks.hidden
                .isEmpty(),
        )
        assertEquals(setOf(BackupSection.SETTINGS), restored.sections)
    }
}
