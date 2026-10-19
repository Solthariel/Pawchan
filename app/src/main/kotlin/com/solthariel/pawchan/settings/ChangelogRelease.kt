package com.solthariel.pawchan.settings

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal data class ChangelogRelease(
    val code: Int,
    val version: String,
    val date: String,
    val sections: List<ChangelogSection>,
)

/** Reads bundled release notes off Main; future releases are not part of the installed app's history. */
internal fun readChangelogHistory(
    installedCode: Int,
    readAsset: (String) -> String,
): List<ChangelogRelease> {
    val metadata = Json.parseToJsonElement(readAsset("changelog-history/versions.json")).jsonObject
    val releases =
        requireNotNull(metadata["versions"]).jsonArray.map { raw ->
            val fields = raw.jsonObject
            val code = requireNotNull(fields["code"]?.jsonPrimitive?.intOrNull)
            val name = requireNotNull(fields["name"]?.jsonPrimitive?.contentOrNull)
            val date = requireNotNull(fields["date"]?.jsonPrimitive?.contentOrNull)
            require(code > 0 && name.isNotBlank() && date.isNotBlank()) { "Invalid release metadata" }
            ChangelogRelease(code, name, date, emptyList())
        }
    require(releases.map { it.code }.distinct().size == releases.size) { "Duplicate release code" }
    require(releases.any { it.code == installedCode }) { "Installed release is missing" }
    return releases
        .filter { it.code <= installedCode }
        .sortedByDescending { it.code }
        .map { release ->
            val sections = parseChangelog(readAsset("changelog-history/changelogs/ru/${release.code}.txt"))
            require(sections.isNotEmpty()) { "Empty release notes: ${release.code}" }
            release.copy(sections = sections)
        }
}
