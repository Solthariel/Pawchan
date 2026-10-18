package com.solthariel.pawchan.backup

import com.solthariel.pawchan.domain.library.ThreadLibraryEntry
import com.solthariel.pawchan.domain.model.BoardAddress
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.domain.model.SiteId
import com.solthariel.pawchan.domain.model.ThreadAddress
import com.solthariel.pawchan.domain.model.ThreadId
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

internal fun stringMapJson(values: Map<String, String>): JsonObject =
    JsonObject(
        values.mapValues { JsonPrimitive(it.value) },
    )

internal fun JsonObject.stringMap(): Map<String, String> =
    mapValues {
        require(it.value.jsonPrimitive.isString)
        it.value.jsonPrimitive.content
    }

internal fun encodeThreads(
    entries: List<ThreadLibraryEntry>,
    history: Boolean,
): String =
    JsonArray(
        entries.map { entry ->
            buildJsonObject {
                put("site", entry.address.site.value)
                put("board", entry.address.board.board.value)
                put("thread", entry.address.thread.value)
                entry.subject?.let { put("subject", it) }
                if (history) put("visited", entry.lastVisitedAtEpochMillis)
            }
        },
    ).toString()

internal fun decodeThreads(
    source: String,
    history: Boolean,
): List<ThreadLibraryEntry> {
    val items = Json.parseToJsonElement(source).jsonArray
    require(items.size <= BACKUP_MAX_RECORDS)
    val result =
        items.map { element ->
            val item = element.jsonObject
            val site = requireNotNull(SiteId.parse(item.getValue("site").jsonPrimitive.content))
            val board = requireNotNull(BoardId.parse(item.getValue("board").jsonPrimitive.content))
            val thread = requireNotNull(ThreadId.parse(item.getValue("thread").jsonPrimitive.long))
            val visited = if (history) item.getValue("visited").jsonPrimitive.long else 0L
            require(!history || visited > 0L)
            ThreadLibraryEntry(
                ThreadAddress(BoardAddress(site, board), thread),
                item["subject"]?.jsonPrimitive?.contentOrNull,
                visited,
                !history,
            )
        }
    require(result.distinctBy { it.address }.size == result.size) { "Повторяющиеся адреса тредов" }
    return result
}
