package com.solthariel.pawchan.backup

import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class BackupJournalDurabilityTest {
    @get:Rule val temporary = TemporaryFolder()
    private val settings = BackupTestSettings()
    private val library = BackupTestLibrary()
    private val stores = BackupStores(settings, library, BackupTestWorkspace())
    private val sync = RecordingDirectorySync()
    private val source =
        ClientBackup(mapOf("theme" to "AMOLED"), emptyList(), WorkspaceState(), setOf(BackupSection.SETTINGS))

    @Test fun commitSynchronizesPreparedJournalBeforeMutationAndRemovalAfterCommit() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            BackupUndoJournal(gate, stores, sync).restore(source)
            assertEquals(listOf(true, false), sync.journalPresence)
            assertEquals("AMOLED", settings.values["theme"])
        }

    @Test fun prepareSyncFailureDoesNotMutateStoresAndStartupRecovers() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            val before = settings.values
            sync.failures = 1
            try {
                BackupUndoJournal(gate, stores, sync).restore(source)
                fail("expected directory sync failure")
            } catch (_: IOException) {
                // No data changes may precede durable undo.
            }
            assertEquals(before, settings.values)
            assertTrue(gate.journal.exists())
            assertThrows(IOException::class.java) { runBlocking { gate.access { fail("journal must block writers") } } }
            BackupUndoJournal(MaintenanceGate(temporary.root), stores, sync).recover()
            assertEquals(before, settings.values)
            assertFalse(gate.journal.exists())
            assertEquals(listOf(true, false), sync.journalPresence)
        }

    @Test fun rollbackSynchronizesJournalRemoval() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            library.failures = 1
            val source = source.copy(sections = setOf(BackupSection.SETTINGS, BackupSection.HISTORY))
            try {
                BackupUndoJournal(gate, stores, sync).restore(source)
                fail("expected storage failure")
            } catch (_: IOException) {
                // expected
            }
            assertEquals(listOf(true, false), sync.journalPresence)
            assertEquals("SYSTEM", settings.values["theme"])
        }
}
