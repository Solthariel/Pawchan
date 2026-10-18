package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.BoardPost
import com.solthariel.pawchan.domain.model.PostAddress
import com.solthariel.pawchan.domain.model.PostBadge
import com.solthariel.pawchan.domain.model.PostContent
import com.solthariel.pawchan.domain.model.PostDocument
import com.solthariel.pawchan.domain.model.PostId
import com.solthariel.pawchan.domain.model.PostLink
import com.solthariel.pawchan.domain.model.PostMetadata
import com.solthariel.pawchan.domain.model.PostTextRun
import com.solthariel.pawchan.domain.model.PostTextStyle
import com.solthariel.pawchan.domain.model.RemoteUrl
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

internal fun BoardPost.encodePost(): JsonObject =
    buildJsonObject {
        put("address", address.thread.encodeAddress())
        put("id", address.post.value)
        put(
            "runs",
            JsonArray(
                content.document.runs.map { run ->
                    buildJsonObject {
                        put("text", run.text)
                        put("styles", JsonArray(run.styles.map { JsonPrimitive(it.name) }))
                        when (val link = run.link) {
                            is PostLink.Reference -> {
                                put("reference", link.post.value)
                                link.thread?.let { put("target", it.encodeAddress()) }
                            }

                            is PostLink.External -> {
                                put("url", link.url.value)
                            }

                            null -> {
                                Unit
                            }
                        }
                    }
                },
            ),
        )
        put("name", content.metadata.name?.let(::JsonPrimitive) ?: JsonNull)
        put("date", content.metadata.date?.let(::JsonPrimitive) ?: JsonNull)
        put("subject", content.metadata.subject?.let(::JsonPrimitive) ?: JsonNull)
        put("sage", content.metadata.sage)
        put("op", content.metadata.originalPoster)
        put("nameColor", content.metadata.nameColor?.let(::JsonPrimitive) ?: JsonNull)
        put(
            "badges",
            JsonArray(
                content.metadata.badges.map {
                    buildJsonObject {
                        put("label", it.label)
                        put("image", it.image.value)
                        put("symbol", it.symbol?.let(::JsonPrimitive) ?: JsonNull)
                    }
                },
            ),
        )
        put("files", JsonArray(attachments.map { it.encodeMedia() }))
    }

internal fun JsonObject.decodePost(): BoardPost {
    val runs =
        requiredValue("runs").jsonArray.map { element ->
            val run = element.jsonObject
            val link =
                when {
                    run["reference"] != null -> {
                        PostLink.Reference(
                            requireNotNull(PostId.parse(run.requiredValue("reference").jsonPrimitive.long)),
                            run["target"]?.jsonObject?.decodeAddress(),
                        )
                    }

                    run["url"] != null -> {
                        PostLink.External(requireNotNull(RemoteUrl.parse(run.workspaceString("url"))))
                    }

                    else -> {
                        null
                    }
                }
            PostTextRun(
                run.workspaceString("text"),
                run
                    .requiredValue("styles")
                    .jsonArray
                    .map {
                        PostTextStyle.valueOf(it.jsonPrimitive.content)
                    }.toSet(),
                link,
            )
        }
    val metadata =
        PostMetadata(
            nullableString("name"),
            nullableString("date"),
            boolean("sage"),
            nullableString("subject"),
            this["nameColor"]?.jsonPrimitive?.intOrNull,
            requiredValue("badges").jsonArray.map { element ->
                val badge = element.jsonObject
                PostBadge(
                    badge.workspaceString("label"),
                    requireNotNull(RemoteUrl.parse(badge.workspaceString("image"))),
                    badge.nullableString("symbol"),
                )
            },
            boolean("op"),
        )
    return BoardPost(
        PostAddress(
            requiredValue("address").jsonObject.decodeAddress(),
            requireNotNull(PostId.parse(requiredValue("id").jsonPrimitive.long)),
        ),
        PostContent(PostDocument(runs), metadata),
        requiredValue("files").jsonArray.map { it.jsonObject.decodeMedia() },
    )
}

internal fun ThreadAddress.encodeAddress(): JsonObject =
    buildJsonObject {
        put("site", site.value)
        put("board", board.board.value)
        put("thread", thread.value)
    }

internal fun JsonObject.decodeAddress(): ThreadAddress =
    ThreadAddress(
        BoardAddress(
            requireNotNull(SiteId.parse(workspaceString("site"))),
            requireNotNull(BoardId.parse(workspaceString("board"))),
        ),
        requireNotNull(ThreadId.parse(requiredValue("thread").jsonPrimitive.long)),
    )

internal fun JsonObject.workspaceString(key: String): String = requiredValue(key).jsonPrimitive.content

internal fun JsonObject.nullableString(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

internal fun JsonObject.boolean(key: String): Boolean = this[key]?.jsonPrimitive?.booleanOrNull == true

internal fun JsonObject.requiredValue(key: String): kotlinx.serialization.json.JsonElement =
    this[key] ?: throw kotlinx.serialization.SerializationException("Missing workspace field: $key")
