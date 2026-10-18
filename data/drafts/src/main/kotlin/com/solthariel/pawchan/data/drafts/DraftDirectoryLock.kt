package com.solthariel.pawchan.data.drafts

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import kotlin.coroutines.coroutineContext

internal suspend fun <T> withDraftDirectoryLock(
    directory: File,
    block: suspend () -> T,
): T {
    val available = directory.isDirectory || directory.mkdirs() || directory.isDirectory
    if (!available) throw IOException("Could not create draft directory")
    return RandomAccessFile(File(directory, "lock"), "rw").use { file ->
        acquireDraftLock(file.channel).use { block() }
    }
}

private suspend fun acquireDraftLock(channel: FileChannel): FileLock {
    var contention: OverlappingFileLockException? = null
    repeat(LOCK_ATTEMPTS) {
        coroutineContext.ensureActive()
        val lock =
            try {
                channel.tryLock()
            } catch (failure: OverlappingFileLockException) {
                contention = failure
                null
            }
        if (lock != null) return lock
        delay(LOCK_RETRY_MILLIS)
    }
    throw IOException("Draft cache is busy; try again", contention)
}

private const val LOCK_ATTEMPTS = 80
private const val LOCK_RETRY_MILLIS = 25L
