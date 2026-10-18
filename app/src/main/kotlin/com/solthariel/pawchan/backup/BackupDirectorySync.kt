package com.solthariel.pawchan.backup

import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import java.io.File
import java.io.IOException

/** Platform boundary: JVM tests model durability; Android synchronizes the real directory. */
internal fun interface BackupDirectorySync {
    fun sync(directory: File)
}

internal object AndroidBackupDirectorySync : BackupDirectorySync {
    override fun sync(directory: File) {
        try {
            val descriptor = Os.open(directory.path, OsConstants.O_RDONLY, 0)
            try {
                Os.fsync(descriptor)
            } finally {
                Os.close(descriptor)
            }
        } catch (failure: ErrnoException) {
            throw IOException("Не удалось синхронизировать каталог восстановления", failure)
        }
    }
}
