package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.ComposerDraftPayload
import com.solthariel.pawchan.domain.drafts.DraftMedia
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

internal class DraftFileStore(
    private val directory: File,
) {
    suspend fun store(draft: ComposerDraftPayload): List<CachedDraftMedia> {
        require(draft.media.size <= PostingRules.MAX_LOCAL_FILES)
        require(
            draft.media.sumOf {
                it.source.content.size
                    .toLong()
            } <= PostingRules.MAX_TOTAL_BYTES,
        )
        return draft.media.map { file ->
            coroutineContext.ensureActive()
            val hash = storeBytes(file.source.content)
            val preview =
                file.preview?.let {
                    require(it.isNotEmpty() && it.size <= MAX_PREVIEW_BYTES)
                    CachedDraftPreview(storeBytes(it), it.size.toLong())
                }
            CachedDraftMedia(
                hash,
                file.source.fileName,
                file.source.mimeType,
                file.source.content.size
                    .toLong(),
                file.options,
                file.dimensions,
                preview,
            )
        }
    }

    suspend fun restore(record: DraftRecord): ComposerDraftPayload {
        require(record.media.sumOf { it.bytes } <= PostingRules.MAX_TOTAL_BYTES)
        val media =
            record.media.map { file ->
                coroutineContext.ensureActive()
                val bytes = readBytes(file.hash, file.bytes)
                val source = File(directory, file.hash)
                val preview = file.preview?.let { readBytes(it.hash, it.bytes) }
                DraftMedia(
                    ReplyAttachment(file.name, file.mime, bytes),
                    file.options,
                    file.dimensions,
                    source.toURI().toString(),
                    preview,
                )
            }
        return ComposerDraftPayload(record.text, record.options, media)
    }

    private suspend fun storeBytes(bytes: ByteArray): String {
        val hash = digest(bytes)
        val target = File(directory, hash)
        if (!target.isFile) atomicWrite(target, bytes)
        return hash
    }

    private fun readBytes(
        hash: String,
        expectedBytes: Long,
    ): ByteArray {
        val source = File(directory, hash)
        if (!source.isFile || source.length() != expectedBytes) throw IOException("Draft attachment is missing")
        val bytes = source.readBytes()
        if (digest(bytes) != hash) throw IOException("Draft attachment is damaged")
        return bytes
    }

    fun cleanup(records: List<DraftRecord>) {
        val referenced = records.flatMap { it.media }.flatMap { listOfNotNull(it.hash, it.preview?.hash) }.toSet()
        directory.listFiles()?.forEach { file ->
            if (file.name !in referenced && !file.delete()) throw IOException("Could not clean draft cache")
        }
    }
}

internal suspend fun atomicWrite(
    target: File,
    bytes: ByteArray,
) {
    val parent = requireNotNull(target.parentFile)
    if (!parent.isDirectory && !parent.mkdirs()) throw IOException("Could not create draft directory")
    val temporary = File(parent, target.name + ".tmp")
    try {
        FileOutputStream(temporary).use { output ->
            var position = 0
            while (position < bytes.size) {
                coroutineContext.ensureActive()
                val length = minOf(DEFAULT_BUFFER_SIZE, bytes.size - position)
                output.write(bytes, position, length)
                position += length
            }
            output.fd.sync()
        }
        coroutineContext.ensureActive()
        Files.move(
            temporary.toPath(),
            target.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING,
        )
    } finally {
        temporary.delete()
    }
}

private fun digest(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        (it.toInt() and BYTE_MASK).toString(HEX_BASE).padStart(2, '0')
    }

private const val BYTE_MASK = 255
private const val HEX_BASE = 16

internal const val MAX_PREVIEW_BYTES = 2 * 1024 * 1024
