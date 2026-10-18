package com.solthariel.pawchan.data.maintenance

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

/** File-backed exclusion shared by foreground, workers and independent repository instances. */
class MaintenanceGate(
    val directory: File,
) {
    val journal = File(directory, "pending.pawbackup")
    private val identity = directory.absoluteFile.normalize().path

    suspend fun <T> access(block: suspend () -> T): T = locked(false, block)

    /** Only the recovery coordinator may enter with an existing undo journal. */
    suspend fun <T> recover(block: suspend () -> T): T = locked(true, block)

    private suspend fun <T> locked(
        recovery: Boolean,
        block: suspend () -> T,
    ): T {
        if (coroutineContext[MaintenanceOwner]?.identity == identity) return block()
        return withContext(Dispatchers.IO) {
            if (!directory.isDirectory && !directory.mkdirs() &&
                !directory.isDirectory
            ) {
                throw IOException("Cannot create maintenance directory")
            }
            RandomAccessFile(File(directory, "lock"), "rw").use { file ->
                acquire(file.channel).use {
                    if (!recovery && journal.exists()) throw IOException("Требуется восстановление прерванной операции")
                    withContext(MaintenanceOwner(identity)) { block() }
                }
            }
        }
    }

    private suspend fun acquire(channel: FileChannel): FileLock {
        while (true) {
            coroutineContext.ensureActive()
            val lock =
                try {
                    channel.tryLock()
                } catch (_: OverlappingFileLockException) {
                    null
                }
            if (lock != null) return lock
            delay(LOCK_RETRY_MILLIS)
        }
    }
}

private const val LOCK_RETRY_MILLIS = 25L

private class MaintenanceOwner(
    val identity: String,
) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<MaintenanceOwner>
}
