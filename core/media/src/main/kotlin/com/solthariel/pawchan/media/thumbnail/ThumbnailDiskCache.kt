package com.solthariel.pawchan.media.thumbnail

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicLong

internal class ThumbnailDiskCache(
    private val directory: File,
    private val maxSizeBytes: Long,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    private val temporaryFileCounter = AtomicLong()
    private var pruneJob: Job? = null
    private var sizeBytes = -1L
    private val initialized = CountDownLatch(1)

    init {
        pruneJob =
            scope.launch {
                try {
                    directory.mkdirs()
                    initializeAndPrune()
                } finally {
                    initialized.countDown()
                }
            }
    }

    fun load(key: String): Bitmap? {
        initialized.await()
        val file = File(directory, key)
        return synchronized(lock) {
            if (!file.isFile) return@synchronized null
            try {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)).also {
                    file.setLastModified(System.currentTimeMillis())
                }
            } catch (exception: IOException) {
                Log.w("PawchanThumbnail", "Corrupt cached thumbnail: $file", exception)
                val length = file.length()
                if (file.delete() && sizeBytes >= 0L) sizeBytes -= length
                null
            }
        }
    }

    fun store(
        key: String,
        bitmap: Bitmap,
    ) {
        initialized.await()
        directory.mkdirs()
        val file = File(directory, key)
        val temporary = File(directory, "$key.${temporaryFileCounter.incrementAndGet()}.tmp")
        try {
            FileOutputStream(temporary).use { output ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, PNG_COMPRESSION_QUALITY, output)) {
                    throw IOException("Bitmap compression failed")
                }
                output.fd.sync()
            }
            var shouldPrune = false
            synchronized(lock) {
                val previousLength = if (file.isFile) file.length() else 0L
                Files.move(
                    temporary.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
                file.setLastModified(System.currentTimeMillis())
                if (sizeBytes >= 0L) {
                    sizeBytes += file.length() - previousLength
                    shouldPrune = sizeBytes > maxSizeBytes
                }
            }
            if (shouldPrune) schedulePrune()
        } catch (exception: IOException) {
            Log.w("PawchanThumbnail", "Failed to cache thumbnail: $file", exception)
            temporary.delete()
        }
    }

    private fun schedulePrune() {
        synchronized(lock) {
            if (pruneJob?.isActive == true) return
            pruneJob = scope.launch { pruneIfNeeded() }
        }
    }

    private fun initializeAndPrune() {
        synchronized(lock) {
            val allFiles = directory.listFiles()?.toList().orEmpty()
            val files = allFiles.filter { it.isFile && !it.name.endsWith(".tmp") }
            allFiles.filter { it.name.endsWith(".tmp") }.forEach(File::delete)
            sizeBytes = files.sumOf { it.length() }
            pruneLocked(files)
        }
    }

    private fun pruneIfNeeded() {
        synchronized(lock) {
            if (sizeBytes <= maxSizeBytes) return
            val files = directory.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") } ?: return
            pruneLocked(files)
        }
    }

    private fun pruneLocked(files: List<File>) {
        if (sizeBytes <= maxSizeBytes) return
        val targetSize = (maxSizeBytes * PRUNE_NUMERATOR) / PRUNE_DENOMINATOR
        for (file in files.sortedBy { it.lastModified() }) {
            val length = file.length()
            if (file.delete()) sizeBytes -= length
            if (sizeBytes <= targetSize) break
        }
    }

    fun clear() {
        initialized.await()
        synchronized(lock) {
            directory.listFiles()?.filter { it.isFile && !it.name.endsWith(".tmp") }?.forEach {
                if (!it.delete() && it.exists()) throw IOException("Cannot remove cached thumbnail")
            }
            sizeBytes = 0L
        }
    }

    override fun close() {
        initialized.countDown()
        scope.cancel()
    }
}

private const val PNG_COMPRESSION_QUALITY = 100
private const val PRUNE_NUMERATOR = 9L
private const val PRUNE_DENOMINATOR = 10L
