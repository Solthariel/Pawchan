package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.AttachmentId
import com.solthariel.pawchan.domain.model.AttachmentSource
import com.solthariel.pawchan.domain.model.ByteSize
import com.solthariel.pawchan.domain.model.MediaAttachment
import com.solthariel.pawchan.domain.model.MediaDimensions
import com.solthariel.pawchan.domain.model.MediaKind
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.ThreadFlags
import com.solthariel.pawchan.domain.model.ThreadSnapshot
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal fun MediaAttachment.encodeMedia(): JsonObject =
    buildJsonObject {
        put("id", id.value)
        put("name", fileName)
        put("url", remoteUrl.value)
        put("thumbnail", thumbnailUrl?.value?.let(::JsonPrimitive) ?: JsonNull)
        put("bytes", byteSize?.value?.let(::JsonPrimitive) ?: JsonNull)
        put("kind", kind.name)
        put("sticker", isSticker)
        put("width", dimensions?.width?.let(::JsonPrimitive) ?: JsonNull)
        put("height", dimensions?.height?.let(::JsonPrimitive) ?: JsonNull)
    }

internal fun JsonObject.decodeMedia(): MediaAttachment =
    MediaAttachment(
        requireNotNull(AttachmentId.parse(workspaceString("id"))),
        workspaceString("name"),
        AttachmentSource(
            requireNotNull(RemoteUrl.parse(workspaceString("url"))),
            nullableString("thumbnail")?.let(RemoteUrl::parse),
        ),
        this["bytes"]?.jsonPrimitive?.longOrNull?.let(ByteSize::parse),
        MediaKind.valueOf(workspaceString("kind")),
        MediaDimensions.create(this["width"]?.jsonPrimitive?.intOrNull, this["height"]?.jsonPrimitive?.intOrNull),
        isSticker = this["sticker"]?.jsonPrimitive?.booleanOrNull ?: legacyDvachSticker(workspaceString("url")),
    )

internal fun ThreadSnapshot.encodeSnapshot(): JsonObject =
    buildJsonObject {
        put("address", address.encodeAddress())
        put("subject", subject?.let(::JsonPrimitive) ?: JsonNull)
        put("closed", flags.closed)
        put("pinned", flags.pinned)
        put("archived", flags.archived)
        put("endless", flags.endless)
        put("unique", uniquePosters)
        put("subjects", subjectsEnabled?.let(::JsonPrimitive) ?: JsonNull)
        put("posts", JsonArray(posts.map { it.encodePost() }))
        tag?.let { put("tag", it) }
        if (deletedPosts.isNotEmpty()) put("deleted", JsonArray(deletedPosts.map { JsonPrimitive(it.value) }))
    }

internal fun JsonObject.decodeSnapshot(): ThreadSnapshot {
    val snapshot =
        ThreadSnapshot(
            requiredValue("address").jsonObject.decodeAddress(),
            nullableString("subject"),
            ThreadFlags(boolean("pinned"), boolean("closed"), boolean("archived"), boolean("endless")),
            requiredValue("posts").jsonArray.map { it.jsonObject.decodePost() },
            requiredValue("unique").jsonPrimitive.int,
            this["subjects"]?.jsonPrimitive?.booleanOrNull,
            nullableString("tag"),
        )
    val deleted =
        this["deleted"]
            ?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.longOrNull?.let(PostId::parse) }
            ?.toSet()
            .orEmpty()
    return if (deleted.isEmpty()) snapshot else snapshot.withPosts(snapshot.posts, deleted)
}

private fun legacyDvachSticker(url: String): Boolean {
    val uri = runCatching { java.net.URI(url) }.getOrNull() ?: return false
    return uri.host in setOf("2ch.hk", "2ch.su", "2ch.life") && uri.path.orEmpty().startsWith("/stickers/")
}
