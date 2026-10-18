package com.solthariel.pawchan.backup

import java.io.File
import java.io.IOException

/** Records the durability boundary without invoking Android or unsupported Windows directory channels. */
internal class RecordingDirectorySync : BackupDirectorySync {
    val journalPresence = mutableListOf<Boolean>()
    var failures = 0

    override fun sync(directory: File) {
        journalPresence += File(directory, "pending.pawbackup").exists()
        if (failures > 0) {
            failures--
            throw IOException("simulated directory sync failure")
        }
    }
}
