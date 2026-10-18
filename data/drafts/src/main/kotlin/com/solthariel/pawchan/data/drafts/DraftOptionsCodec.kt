package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.drafts.DraftImageReencoding
import com.solthariel.pawchan.domain.drafts.MediaDraftOptions
import com.solthariel.pawchan.domain.posting.PostingOptions
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

internal fun encodePostingOptions(options: PostingOptions): JsonObject =
    buildJsonObject {
        put("name", options.name)
        put("sage", options.sage)
        put("author", options.threadAuthor)
        options.icon?.let { put("icon", it) }
        put("tag", options.tag)
    }

internal fun decodePostingOptions(root: JsonObject): PostingOptions =
    PostingOptions(
        root.string("name"),
        root.flag("sage"),
        root.flag("author"),
        (root["icon"] as? JsonPrimitive)?.intOrNull,
        // Старые черновики могут содержать ключ "sticker": отправка стикеров удалена, ключ игнорируется.
        (root["tag"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
    )

internal fun encodeMediaOptions(options: MediaDraftOptions): JsonObject =
    buildJsonObject {
        put("unique", options.uniqueHash)
        put("metadata", options.removeMetadata)
        put("removeName", options.removeFileName)
        options.customName?.let { put("name", it) }
        put("spoiler", options.spoiler)
        options.reencoding?.let { put("reencoding", encodeReencoding(it)) }
        options.previousReencoding?.let { put("previousReencoding", encodeReencoding(it)) }
    }

internal fun decodeMediaOptions(root: JsonObject): MediaDraftOptions =
    MediaDraftOptions(
        uniqueHash = root.flag("unique"),
        removeMetadata = root.flag("metadata"),
        removeFileName = root.flag("removeName"),
        customName = (root["name"] as? JsonPrimitive)?.contentOrNull,
        spoiler = root.flag("spoiler"),
        reencoding = (root["reencoding"] as? JsonObject)?.let(::decodeReencoding),
        previousReencoding = (root["previousReencoding"] as? JsonObject)?.let(::decodeReencoding),
    )

internal fun JsonObject.string(key: String): String =
    requireNotNull(
        (get(key) as? JsonPrimitive)?.contentOrNull,
    ) { "Invalid draft text" }

internal fun JsonObject.flag(key: String): Boolean = (get(key) as? JsonPrimitive)?.booleanOrNull ?: false

internal fun JsonObject.integer(key: String): Int = requireNotNull((get(key) as? JsonPrimitive)?.intOrNull)

private fun encodeReencoding(settings: DraftImageReencoding): JsonObject =
    buildJsonObject {
        put("jpeg", settings.jpeg)
        put("quality", settings.quality)
        put("scale", settings.scalePercent)
        put("scaleDivisor", settings.scaleDivisor)
    }

private fun decodeReencoding(root: JsonObject): DraftImageReencoding =
    DraftImageReencoding(
        jpeg = root.flag("jpeg"),
        quality = root.integer("quality"),
        scalePercent = root.integer("scale"),
        scaleDivisor = (root["scaleDivisor"] as? JsonPrimitive)?.intOrNull ?: 1,
    )
