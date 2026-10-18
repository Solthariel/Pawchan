package com.solthariel.pawchan.backup

import com.solthariel.pawchan.data.maintenance.MaintenanceGate
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.ThreadMarks
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

class BackupUndoJournalTest {
    @get:Rule val temporary = TemporaryFolder()
    private val settings = BackupTestSettings()
    private val library = BackupTestLibrary()
    private val workspace = BackupTestWorkspace()
    private val stores = BackupStores(settings, library, workspace)
    private val address =
        ThreadAddress(
            BoardAddress(requireNotNull(SiteId.parse("dvach")), requireNotNull(BoardId.parse("b"))),
            requireNotNull(ThreadId.parse(42L)),
        )

    @Test fun successfulReplacementDeletesJournalAndPreservesUnselectedData() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            workspace.state =
                WorkspaceState(
                    tabs = listOf(OpenThread(address, "open")),
                    marks = ThreadMarks(hidden = setOf(address)),
                )
            val source =
                ClientBackup(
                    mapOf("theme" to "AMOLED"),
                    emptyList(),
                    WorkspaceState(),
                    setOf(BackupSection.SETTINGS),
                )
            journal(gate).restore(source)
            assertEquals("AMOLED", settings.values["theme"])
            assertEquals(setOf(address), workspace.state.marks.hidden)
            assertEquals(1, workspace.state.tabs.size)
            assertFalse(gate.journal.exists())
        }

    @Test fun failureAfterSettingsMutationRollsBackEverySection() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            val before = settings.values
            library.failures = 1
            try {
                journal(gate).restore(
                    ClientBackup(
                        mapOf("theme" to "AMOLED"),
                        emptyList(),
                        WorkspaceState(),
                        setOf(BackupSection.SETTINGS, BackupSection.HISTORY),
                    ),
                )
                fail("expected I/O failure")
            } catch (_: IOException) {
                // expected
            }
            assertEquals(before, settings.values)
            assertFalse(gate.journal.exists())
        }

    @Test fun failedRollbackLeavesDurableJournalAndNextStartupRecovers() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            val before = settings.values
            library.failures = 2
            val source =
                ClientBackup(
                    mapOf("theme" to "AMOLED"),
                    emptyList(),
                    WorkspaceState(),
                    setOf(BackupSection.SETTINGS, BackupSection.HISTORY),
                )
            try {
                journal(gate).restore(source)
                fail("expected failure")
            } catch (_: IOException) {
                // expected
            }
            assertTrue(gate.journal.exists())
            journal(MaintenanceGate(temporary.root)).recover()
            assertEquals(before, settings.values)
            assertFalse(gate.journal.exists())
        }

    @Test fun failureAfterWorkspaceMutationRestoresTabsAndFullMarks() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            val before =
                WorkspaceState(
                    tabs = listOf(OpenThread(address, "open", 43L, 10)),
                    marks = ThreadMarks(hidden = setOf(address), seen = mapOf(address to 44L)),
                )
            workspace.state = before
            workspace.failAfterWrite = true
            try {
                journal(gate).restore(
                    ClientBackup(emptyMap(), emptyList(), WorkspaceState(), setOf(BackupSection.HIDDEN)),
                )
                fail("expected failure")
            } catch (_: IOException) {
                // expected
            }
            assertEquals(before, workspace.state)
            assertFalse(gate.journal.exists())
        }

    @Test fun invalidSettingsDoNotCreateJournalOrMutateAnything() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            val before = settings.values
            try {
                journal(gate).restore(
                    ClientBackup(
                        mapOf("unknown" to "value"),
                        emptyList(),
                        WorkspaceState(),
                        setOf(BackupSection.SETTINGS),
                    ),
                )
                fail("expected validation failure")
            } catch (_: IllegalArgumentException) {
                // expected
            }
            assertEquals(before, settings.values)
            assertFalse(gate.journal.exists())
        }

    @Test fun cancellationAfterUndoWaitsForDurableCommit() =
        runBlocking {
            val gate = MaintenanceGate(temporary.root)
            val entered = CompletableDeferred<Unit>()
            val release = CompletableDeferred<Unit>()
            library.onReplace = {
                entered.complete(Unit)
                release.await()
            }
            val job =
                launch {
                    journal(gate).restore(
                        ClientBackup(
                            mapOf("theme" to "AMOLED"),
                            emptyList(),
                            WorkspaceState(),
                            setOf(BackupSection.SETTINGS, BackupSection.HISTORY),
                        ),
                    )
                }
            entered.await()
            job.cancel()
            assertTrue(gate.journal.exists())
            release.complete(Unit)
            job.join()
            assertEquals("AMOLED", settings.values["theme"])
            assertFalse(gate.journal.exists())
        }

    private fun journal(gate: MaintenanceGate) = BackupUndoJournal(gate, stores, RecordingDirectorySync())

    @Test fun emptySelectedHiddenSectionClearsOnlyHiddenThreads() {
        val current =
            WorkspaceState(
                tabs = listOf(OpenThread(address, "open")),
                marks = ThreadMarks(hidden = setOf(address)),
            )
        val backup = ClientBackup(emptyMap(), emptyList(), WorkspaceState(), setOf(BackupSection.HIDDEN))
        val replaced = replaceWorkspace(current, backup)
        assertTrue(replaced.marks.hidden.isEmpty())
        assertEquals(current.tabs, replaced.tabs)
    }
}
