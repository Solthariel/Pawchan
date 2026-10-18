package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import com.solthariel.pawchan.domain.thread.ThreadHistoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.File
import java.io.IOException

/**
 * История просмотренных тредов: по файлу на тред, хранится [MAX_THREADS] последних.
 * Повреждённый файл считается отсутствующим — это кэш, а не пользовательские данные.
 */
class FileThreadHistoryRepository(
    private val directory: File,
) : ThreadHistoryRepository {
    private val mutex = Mutex()

    override suspend fun load(address: ThreadAddress): ThreadSnapshot? =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val file = file(address)
                if (!file.exists() || file.length() > MAX_THREAD_BYTES) return@withLock null
                try {
                    Json
                        .parseToJsonElement(file.readText())
                        .jsonObject
                        .decodeSnapshot()
                        .takeIf { it.address == address }
                } catch (_: IOException) {
                    null
                } catch (_: SerializationException) {
                    null
                } catch (_: IllegalArgumentException) {
                    null
                }
            }
        }

    override suspend fun save(snapshot: ThreadSnapshot) {
        withContext(Dispatchers.IO) {
            mutex.withLock {
                val bytes = snapshot.encodeSnapshot().toString().toByteArray()
                if (bytes.size > MAX_THREAD_BYTES) return@withLock
                try {
                    atomicWrite(file(snapshot.address), bytes)
                    prune()
                } catch (_: IOException) {
                    // История — кэш: не удалось записать, значит тред просто не запомнится.
                }
            }
        }
    }

    override suspend fun erase(address: ThreadAddress) {
        withContext(Dispatchers.IO) {
            mutex.withLock { file(address).delete() }
        }
    }

    private fun file(address: ThreadAddress): File =
        File(directory, "${address.board.site.value}_${address.board.board.value}_${address.thread.value}.json")

    private fun prune() {
        val files = directory.listFiles { file -> file.extension == "json" }.orEmpty()
        if (files.size <= MAX_THREADS) return
        files.sortedByDescending(File::lastModified).drop(MAX_THREADS).forEach(File::delete)
    }
}

private const val MAX_THREADS = 200
private const val MAX_THREAD_BYTES = 16 * 1024 * 1024
