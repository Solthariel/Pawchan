package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.thread.ThreadDeletionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.security.MessageDigest

/** Markers are separate from cached posts: clearing history cannot resurrect a deleted thread. */
class FileThreadDeletionStore(
    private val directory: File,
) : ThreadDeletionStore {
    override suspend fun contains(address: ThreadAddress): Boolean =
        withContext(Dispatchers.IO) {
            try {
                file(address).let { it.isFile && it.length() == MARKER.length.toLong() && it.readText() == MARKER }
            } catch (_: IOException) {
                false
            } catch (_: SecurityException) {
                false
            }
        }

    override suspend fun markDeleted(address: ThreadAddress) {
        withContext(Dispatchers.IO) {
            try {
                atomicWrite(file(address), MARKER.toByteArray())
            } catch (_: IOException) {
                // The network guard retains the confirmed status in memory if disk is unavailable.
            } catch (_: SecurityException) {
                // No storage access; never turn this into a different network failure.
            }
        }
    }

    private fun file(address: ThreadAddress): File = File(directory, deletionKey(address) + ".deleted")

    private fun deletionKey(address: ThreadAddress): String {
        val value = "${address.board.site.value}\n${address.board.board.value}\n${address.thread.value}"
        return MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
    }
}

private const val MARKER = "deleted\n"
