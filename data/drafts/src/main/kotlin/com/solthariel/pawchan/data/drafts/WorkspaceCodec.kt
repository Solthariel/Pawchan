package com.solthariel.pawchan.data.drafts

import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.workspace.OpenThread
import com.solthariel.pawchan.domain.workspace.StoredPost
import com.solthariel.pawchan.domain.workspace.StoredReply
import com.solthariel.pawchan.domain.workspace.ThreadCopy
import com.solthariel.pawchan.domain.workspace.ThreadMarks
import com.solthariel.pawchan.domain.workspace.WorkspaceState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put

internal fun encodeWorkspace(state: WorkspaceState): String =
    buildJsonObject {
        put("version", 1)
        put("route", state.lastRoute?.let(::JsonPrimitive) ?: JsonNull)
        put(
            "posts",
            JsonArray(
                state.posts.map {
                    buildJsonObject {
                        put("post", it.post.encodePost())
                        put("title", it.title?.let(::JsonPrimitive) ?: JsonNull)
                        put("own", it.own)
                        put("tracking", it.tracking)
                    }
                },
            ),
        )
        put(
            "replies",
            JsonArray(
                state.replies.map {
                    buildJsonObject {
                        put("post", it.post.encodePost())
                        put("title", it.title?.let(::JsonPrimitive) ?: JsonNull)
                        put("unread", it.unread)
                    }
                },
            ),
        )
        put(
            "tabs",
            JsonArray(
                state.tabs.map {
                    buildJsonObject {
                        put("address", it.address.encodeAddress())
                        put("title", it.title?.let(::JsonPrimitive) ?: JsonNull)
                        put("postId", it.postId?.let(::JsonPrimitive) ?: JsonNull)
                        put("offset", it.offset)
                    }
                },
            ),
        )
        put("hiddenThreads", JsonArray(state.marks.hidden.map { it.encodeAddress() }))
        put("arhivach", encodeArhivachLinks(state.marks.arhivach))
        put("seen", encodeSeenThreads(state.marks.seen))
        put("readingReturns", encodeReadingReturns(state.marks.readingReturns))
        put("dismissedReplies", JsonArray(state.marks.dismissedReplies.map { it.encodeAddress() }))
        put("media", JsonArray(state.media.map { it.encodeMedia() }))
        put(
            "copies",
            JsonArray(
                state.copies.map {
                    it.encodeCopy()
                },
            ),
        )
    }.toString()

internal fun decodeWorkspace(text: String): WorkspaceState {
    val root = Json.parseToJsonElement(text).jsonObject
    require(root.requiredValue("version").jsonPrimitive.int == 1) { "Unsupported workspace version" }
    return WorkspaceState(
        posts =
            root.requiredValue("posts").jsonArray.map { element ->
                val item = element.jsonObject
                StoredPost(
                    item.requiredValue("post").jsonObject.decodePost(),
                    item.nullableString("title"),
                    item.boolean("own"),
                    item["tracking"]?.jsonPrimitive?.booleanOrNull != false,
                )
            },
        replies =
            root.requiredValue("replies").jsonArray.map { element ->
                val item = element.jsonObject
                StoredReply(
                    item.requiredValue("post").jsonObject.decodePost(),
                    item.nullableString("title"),
                    item.boolean("unread"),
                )
            },
        tabs =
            root.requiredValue("tabs").jsonArray.map { element ->
                val item = element.jsonObject
                OpenThread(
                    item.requiredValue("address").jsonObject.decodeAddress(),
                    item.nullableString("title"),
                    item["postId"]?.jsonPrimitive?.longOrNull,
                    item.requiredValue("offset").jsonPrimitive.int,
                )
            },
        media = root.requiredValue("media").jsonArray.map { it.jsonObject.decodeMedia() },
        copies =
            root.requiredValue("copies").jsonArray.map { element ->
                val item = element.jsonObject
                item.decodeCopy()
            },
        lastRoute = root.nullableString("route"),
        marks = root.decodeMarks(),
    )
}

private fun encodeSeenThreads(seen: Map<ThreadAddress, Long>): JsonArray =
    JsonArray(
        seen.map { (address, post) ->
            buildJsonObject {
                put("address", address.encodeAddress())
                put("post", post)
            }
        },
    )

private fun decodeSeenThreads(element: kotlinx.serialization.json.JsonElement?): Map<ThreadAddress, Long> =
    element
        ?.jsonArray
        ?.associate { item ->
            val value = item.jsonObject
            value.requiredValue("address").jsonObject.decodeAddress() to value.requiredValue("post").jsonPrimitive.long
        }.orEmpty()

private fun encodeArhivachLinks(links: Map<ThreadAddress, String>): JsonArray =
    JsonArray(
        links.map { (address, url) ->
            buildJsonObject {
                put("address", address.encodeAddress())
                put("url", url)
            }
        },
    )

private fun kotlinx.serialization.json.JsonObject.decodeMarks(): ThreadMarks =
    ThreadMarks(
        hidden =
            this["hiddenThreads"]
                ?.jsonArray
                ?.map { it.jsonObject.decodeAddress() }
                ?.toSet()
                .orEmpty(),
        arhivach =
            this["arhivach"]
                ?.jsonArray
                ?.associate { element ->
                    val item = element.jsonObject
                    item.requiredValue("address").jsonObject.decodeAddress() to
                        item.requiredValue("url").jsonPrimitive.content
                }.orEmpty(),
        seen = decodeSeenThreads(this["seen"]),
        readingReturns = decodeReadingReturns(this["readingReturns"]),
        dismissedReplies =
            this["dismissedReplies"]
                ?.jsonArray
                ?.map { it.jsonObject.decodeAddress() }
                ?.toSet()
                .orEmpty(),
    )
