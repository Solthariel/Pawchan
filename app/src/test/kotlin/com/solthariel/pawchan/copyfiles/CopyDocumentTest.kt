package com.solthariel.pawchan.copyfiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyDocumentTest {
    @Test fun backupListNeverTreatsThreadZipsAsRestorableApplicationBackups() {
        assertTrue(copyDocumentMatches("Pawchan.pawbackup", CopyFileKind.BACKUP))
        assertTrue(copyDocumentMatches("Pawchan.PAWBACKUP", CopyFileKind.BACKUP))
        assertFalse(copyDocumentMatches("thread.zip", CopyFileKind.BACKUP))
        assertFalse(copyDocumentMatches("Pawchan.pawbackup.tmp", CopyFileKind.BACKUP))
    }

    @Test fun threadArchivesHaveASeparateKindAndFolder() {
        assertTrue(copyDocumentMatches("thread.zip", CopyFileKind.THREAD))
        assertFalse(copyDocumentMatches("Pawchan.pawbackup", CopyFileKind.THREAD))
        assertEquals("Archive", CopyFileKind.THREAD.folder)
        assertEquals("Backups", CopyFileKind.BACKUP.folder)
    }

    @Test fun copiesAreNewestFirstWithStableNameTieBreaks() {
        val a = document("b", 10)
        val b = document("a", 10)
        val c = document("c", 20)
        assertEquals(listOf(c, b, a), sortedCopyDocuments(listOf(a, c, b)))
    }

    private fun document(
        name: String,
        time: Long,
    ) = CopyDocument("content://test/$name", name, "application/zip", time, 40)
}
