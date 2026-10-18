package com.solthariel.pawchan.backup

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files

class BackupDirectorySyncTest {
    @Test fun nativeDirectorySyncSupportsPrepareAndDelete() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val directory = Files.createTempDirectory(cache.toPath(), "backup-sync-").toFile()
        try {
            val pending = File(directory, "pending.pawbackup")
            pending.writeText("undo")
            AndroidBackupDirectorySync.sync(directory)
            Files.delete(pending.toPath())
            AndroidBackupDirectorySync.sync(directory)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun missingDirectoryIsNotSilentlyAccepted() {
        val cache = InstrumentationRegistry.getInstrumentation().targetContext.cacheDir
        val directory = Files.createTempDirectory(cache.toPath(), "backup-sync-").toFile()
        Files.delete(directory.toPath())
        assertThrows(IOException::class.java) { AndroidBackupDirectorySync.sync(directory) }
    }
}
