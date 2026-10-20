package com.solthariel.pawchan.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** A published, self-contained history; never reads the next release's publishing directory. */
internal fun decodeRemoteChangelog(source: String): List<ChangelogRelease> {
    require(source.length <= MAX_HISTORY_BYTES) { "History exceeds limit" }
    val root = Json.parseToJsonElement(source).jsonObject
    require(root["schema"]?.jsonPrimitive?.intOrNull == 1) { "Unsupported history schema" }
    val rows = requireNotNull(root["releases"]).jsonArray
    require(rows.size in 1..MAX_HISTORY_RELEASES) { "Invalid history length" }
    val releases =
        rows.map { raw ->
            val row = raw.jsonObject
            val code = requireNotNull(row["code"]?.jsonPrimitive?.intOrNull)
            val name = requireNotNull(row["name"]?.jsonPrimitive?.contentOrNull)
            val date = requireNotNull(row["date"]?.jsonPrimitive?.contentOrNull)
            val sections = parseChangelog(requireNotNull(row["notes"]?.jsonPrimitive?.contentOrNull))
            require(code > 0 && RELEASE_NAME.matches(name) && RELEASE_DATE.matches(date) && sections.isNotEmpty())
            ChangelogRelease(code, name, date, sections)
        }
    require(releases.distinctBy { it.code }.size == releases.size) { "Duplicate release code" }
    require(releases.distinctBy { it.version }.size == releases.size) { "Duplicate release name" }
    return releases.sortedByDescending { it.code }
}

internal const val MAX_HISTORY_BYTES = 2 * 1024 * 1024
private const val MAX_HISTORY_RELEASES = 500
private val RELEASE_NAME = Regex("[0-9]+[.][0-9]+[.][0-9]+")
private val RELEASE_DATE = Regex("[0-9]{2}[.][0-9]{2}[.][0-9]{4}")
