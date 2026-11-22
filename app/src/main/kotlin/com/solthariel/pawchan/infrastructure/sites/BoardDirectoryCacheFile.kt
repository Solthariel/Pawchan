package com.solthariel.pawchan.infrastructure.sites

import com.solthariel.pawchan.domain.boards.BoardDirectoryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** A bounded, versioned snapshot, replaced atomically after a complete write. */
internal class BoardDirectoryCacheFile(
    private val site: SiteId,
    private val file: File,
) {
    fun read(): List<BoardDirectoryEntry>? =
        try {
            if (!file.isFile || file.length() > MAX_BYTES) null else readFile()
        } catch (_: IOException) {
            null
        } catch (_: SecurityException) {
            null
        }

    private fun readFile(): List<BoardDirectoryEntry>? =
        DataInputStream(file.inputStream().buffered()).use { input ->
            if (input.readInt() != VERSION || input.readUTF() != site.value) return null
            val size = input.readInt()
            if (size !in 1..MAX_BOARDS) return null
            List(size) {
                val board = BoardId.parse(input.readUTF()) ?: return null
                BoardDirectoryEntry(
                    BoardAddress(site, board),
                    input.readUTF(),
                    input.readUTF(),
                    input.readInt(),
                    input.readBoolean(),
                    input.readBoolean(),
                )
            }.takeIf { input.read() == -1 }
        }

    fun write(boards: List<BoardDirectoryEntry>) {
        var temp: File? = null
        try {
            val parent = file.parentFile ?: return
            if (boards.size !in 1..MAX_BOARDS) return
            if (!parent.isDirectory) parent.mkdirs()
            temp = File.createTempFile("boards-", ".tmp", parent)
            DataOutputStream(temp.outputStream().buffered()).use { output ->
                output.writeInt(VERSION)
                output.writeUTF(site.value)
                output.writeInt(boards.size)
                boards.forEach { board ->
                    output.writeUTF(board.address.board.value)
                    output.writeUTF(board.title)
                    output.writeUTF(board.category)
                    output.writeInt(board.postsPerHour)
                    output.writeBoolean(board.postingEnabled)
                    output.writeBoolean(board.requiresPostingAccess)
                }
            }
            if (temp.length() <= MAX_BYTES) {
                Files.move(
                    temp.toPath(),
                    file.toPath(),
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }
        } catch (_: IOException) {
            // A failed cache write must never break a successful network result.
        } catch (_: SecurityException) {
            // Continue with the in-memory snapshot if storage is unavailable.
        } finally {
            try {
                temp?.delete()
            } catch (_: SecurityException) {
                // A temporary file may remain if storage access was revoked.
            }
        }
    }
}

private const val VERSION = 1
private const val MAX_BOARDS = 10_000
private const val MAX_BYTES = 4 * 1024 * 1024L
