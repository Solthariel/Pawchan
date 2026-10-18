package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.archive.ArchiveMediaState
import com.solthariel.pawchan.domain.archive.ArchivePhase
import com.solthariel.pawchan.domain.archive.ArchiveState
import com.solthariel.pawchan.domain.archive.ArchivedMedia
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal fun ThreadCopy.encodeCopy(): JsonObject =
    buildJsonObject {
        put("snapshot", snapshot.encodeSnapshot())
        put("time", savedAt)
        put("archive", archive.encodeArchive())
        put(
            "position",
            position?.let { position ->
                buildJsonObject {
                    put("post", position.postId?.let(::JsonPrimitive) ?: JsonNull)
                    put("offset", position.offset)
                }
            } ?: JsonNull,
        )
    }

internal fun JsonObject.decodeCopy(): ThreadCopy {
    val snapshot = requiredValue("snapshot").jsonObject.decodeSnapshot()
    val position =
        this["position"]?.takeUnless { it == JsonNull }?.jsonObject?.let {
            OpenThread(
                snapshot.address,
                snapshot.subject,
                it["post"]?.jsonPrimitive?.longOrNull,
                it["offset"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(0) ?: 0,
            )
        }
    return ThreadCopy(
        snapshot,
        requiredValue("time").jsonPrimitive.long,
        this["archive"]?.jsonObject?.decodeArchive() ?: ArchiveState(),
        position,
    )
}

private fun ArchiveState.encodeArchive(): JsonObject =
    buildJsonObject {
        put("media", includeMedia)
        put("url", sourceUrl?.let(::JsonPrimitive) ?: JsonNull)
        put("checked", checkedAt)
        put("phase", phase.name)
        put("message", message?.let(::JsonPrimitive) ?: JsonNull)
        put("mediaFailure", media.failure?.let(::JsonPrimitive) ?: JsonNull)
        put(
            "files",
            JsonArray(
                files.map {
                    buildJsonObject {
                        put("url", it.url)
                        put("file", it.file)
                        put("bytes", it.bytes)
                    }
                },
            ),
        )
    }

private fun JsonObject.decodeArchive(): ArchiveState =
    ArchiveState(
        this["media"]?.jsonPrimitive?.booleanOrNull == true,
        nullableString("url"),
        this["checked"]?.jsonPrimitive?.longOrNull ?: 0,
        this["phase"]?.jsonPrimitive?.content?.let(ArchivePhase::valueOf) ?: ArchivePhase.LOCAL,
        nullableString("message"),
        ArchiveMediaState(
            this["files"]
                ?.jsonArray
                ?.map {
                    val item = it.jsonObject
                    ArchivedMedia(
                        item.workspaceString("url"),
                        item.workspaceString("file"),
                        item.requiredValue("bytes").jsonPrimitive.long,
                    )
                }.orEmpty(),
            nullableString("mediaFailure"),
        ),
    )
