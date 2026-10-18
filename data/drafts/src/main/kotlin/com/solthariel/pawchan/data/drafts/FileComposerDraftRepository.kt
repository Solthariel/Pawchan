package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.ComposerDraftKey
import com.solthariel.pawchan.domain.drafts.ComposerDraftPayload
import com.solthariel.pawchan.domain.drafts.ComposerDraftRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Bounded metadata is read under both locks; no hidden forms or media arrays are retained. */
class FileComposerDraftRepository(
    private val directory: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val cacheLimitBytes: Long = DEFAULT_CACHE_BYTES,
) : ComposerDraftRepository {
    private val mutex = Mutex()
    private val media = DraftFileStore(File(directory, "media"))
    private val index = File(directory, "index.json")

    init {
        require(cacheLimitBytes > 0)
    }

    override suspend fun load(key: ComposerDraftKey): ComposerDraftPayload? =
        locked {
            val records = readIndex()
            media.cleanup(records)
            val record = records.firstOrNull { it.key == key.storageKey() }
            if (record == null) {
                null
            } else {
                val restored = media.restore(record)
                commit(records.filterNot { it.key == record.key } + record)
                restored
            }
        }

    override suspend fun save(
        key: ComposerDraftKey,
        draft: ComposerDraftPayload,
    ) = locked {
        val retained = readIndex().filterNot { it.key == key.storageKey() }.toMutableList()
        if (!draft.empty) {
            val record = DraftRecord(key.storageKey(), draft.text, draft.options, media.store(draft))
            require(listOf(record).cacheBytes() <= cacheLimitBytes) { "Draft exceeds cache budget" }
            retained.add(record)
        }
        val bounded = boundDraftRecords(retained, cacheLimitBytes)
        kotlin.coroutines.coroutineContext.ensureActive()
        commit(bounded)
        media.cleanup(bounded)
    }

    override suspend fun remove(key: ComposerDraftKey) =
        locked {
            val retained = readIndex().filterNot { it.key == key.storageKey() }
            commit(retained)
            media.cleanup(retained)
        }

    private suspend fun <T> locked(block: suspend () -> T): T =
        withContext(dispatcher) {
            mutex.withLock { withDraftDirectoryLock(directory, block) }
        }

    private fun readIndex(): List<DraftRecord> {
        if (!index.exists()) return emptyList()
        if (index.length() > MAX_INDEX_BYTES) throw IOException("Draft index exceeds safety limit")
        return decodeStoredDraftIndex(index.readText())
    }

    private suspend fun commit(records: List<DraftRecord>) {
        val bytes = encodeDraftIndex(records).toByteArray()
        require(bytes.size <= MAX_INDEX_BYTES)
        atomicWrite(index, bytes)
    }
}

private fun ComposerDraftKey.storageKey(): String =
    listOf(board.site.value, board.board.value, thread?.value?.toString() ?: "new").joinToString("|")

internal fun boundDraftRecords(
    records: List<DraftRecord>,
    byteLimit: Long,
): List<DraftRecord> {
    val retained = records.toMutableList()
    while (retained.isNotEmpty() &&
        (retained.size > MAX_DRAFTS || retained.cacheBytes() > byteLimit)
    ) {
        retained.removeAt(0)
    }
    return retained
}

private fun List<DraftRecord>.cacheBytes(): Long =
    flatMap { it.media }
        .flatMap {
            listOfNotNull(it.hash to it.bytes, it.preview?.let { preview -> preview.hash to preview.bytes })
        }.distinctBy { it.first }
        .sumOf { it.second }

private const val MAX_INDEX_BYTES = 1024 * 1024
private const val DEFAULT_CACHE_BYTES = 128L * 1024 * 1024
