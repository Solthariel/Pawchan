package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DraftText
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.io.IOException

internal fun encodeDraftIndex(records: List<DraftRecord>): String =
    JsonArray(
        records.map { record ->
            buildJsonObject {
                put("key", record.key)
                put("subject", record.text.subject)
                put("comment", record.text.comment)
                put("email", record.text.email)
                put("options", encodePostingOptions(record.options))
                put("media", JsonArray(record.media.map(::encodeCachedMedia)))
            }
        },
    ).toString()

internal fun decodeDraftIndex(content: String): List<DraftRecord> {
    val root = Json.parseToJsonElement(content) as? JsonArray ?: error("Invalid draft index")
    require(root.size <= MAX_DRAFTS)
    return root.map { value ->
        val item = value as? JsonObject ?: error("Invalid draft entry")
        val media = item["media"] as? JsonArray ?: error("Invalid draft media")
        require(media.size <= PostingRules.MAX_LOCAL_FILES)
        DraftRecord(
            item.string("key"),
            DraftText(item.string("subject"), item.string("comment"), item.string("email")),
            decodePostingOptions(item["options"] as? JsonObject ?: error("Invalid draft options")),
            media.map { decodeCachedMedia(it as? JsonObject ?: error("Invalid draft media")) },
        )
    }
}

private fun encodeCachedMedia(media: CachedDraftMedia): JsonObject =
    buildJsonObject {
        put("hash", media.hash)
        put("name", media.name)
        put("bytes", media.bytes)
        media.mime?.let { put("mime", it) }
        media.dimensions?.let { put("dimensions", it) }
        put("options", encodeMediaOptions(media.options))
        media.preview?.let {
            put(
                "preview",
                buildJsonObject {
                    put("hash", it.hash)
                    put("bytes", it.bytes)
                },
            )
        }
    }

private fun decodeCachedMedia(root: JsonObject): CachedDraftMedia {
    val hash = root.string("hash")
    val bytes = requireNotNull((root["bytes"] as? JsonPrimitive)?.longOrNull)
    require(hash.length == HASH_LENGTH && hash.all { it in '0'..'9' || it in 'a'..'f' })
    require(bytes in 1..ReplyAttachment.MAX_ATTACHMENT_BYTES.toLong())
    return CachedDraftMedia(
        hash,
        root.string("name"),
        (root["mime"] as? JsonPrimitive)?.contentOrNull,
        bytes,
        decodeMediaOptions(root["options"] as? JsonObject ?: error("Invalid media options")),
        (root["dimensions"] as? JsonPrimitive)?.contentOrNull,
        (root["preview"] as? JsonObject)?.let(::decodeCachedPreview),
    )
}

private fun decodeCachedPreview(root: JsonObject): CachedDraftPreview {
    val hash = root.string("hash")
    val bytes = requireNotNull((root["bytes"] as? JsonPrimitive)?.longOrNull)
    require(hash.length == HASH_LENGTH && hash.all { it in '0'..'9' || it in 'a'..'f' })
    require(bytes in 1..MAX_PREVIEW_BYTES.toLong())
    return CachedDraftPreview(hash, bytes)
}

internal const val MAX_DRAFTS = 5
private const val HASH_LENGTH = 64

internal fun decodeStoredDraftIndex(content: String): List<DraftRecord> =
    try {
        decodeDraftIndex(content)
    } catch (failure: IllegalArgumentException) {
        throw IOException("Could not read draft index", failure)
    } catch (failure: IllegalStateException) {
        throw IOException("Could not read draft index", failure)
    }
