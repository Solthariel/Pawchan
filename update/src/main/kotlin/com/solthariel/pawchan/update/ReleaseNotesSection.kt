package com.solthariel.pawchan.update

public data class ReleaseNotesSection(
    val title: String,
    val entries: List<String>,
)

/** Shared by history and update cards; list markers and internal version headers are not UI text. */
public fun parseReleaseNotes(source: String): List<ReleaseNotesSection> {
    val result = mutableListOf<ReleaseNotesSection>()
    var title = "Изменения"
    var entries = mutableListOf<String>()
    source.lineSequence().forEach { raw ->
        val line = raw.trim().removePrefix("\uFEFF").trimStart('#', ' ')
        when {
            line.isBlank() || RELEASE_VERSION_HEADER.containsMatchIn(line) -> {
                Unit
            }

            line.startsWith("* ") || line.startsWith("- ") || line.startsWith("• ") -> {
                line
                    .drop(2)
                    .trim()
                    .takeIf(String::isNotEmpty)
                    ?.let(entries::add)
            }

            raw.startsWith("  ") && entries.isNotEmpty() -> {
                entries[entries.lastIndex] = entries.last() + " " + line
            }

            else -> {
                if (entries.isNotEmpty()) result.add(ReleaseNotesSection(title, entries.toList()))
                title = line.trimStart('#', ' ').removeSuffix(":")
                entries = mutableListOf()
            }
        }
    }
    if (entries.isNotEmpty()) result.add(ReleaseNotesSection(title, entries.toList()))
    return result
}

private val RELEASE_VERSION_HEADER = Regex("^(?:Версия|Version)\\s+\\d+[.]\\d+[.]\\d+", RegexOption.IGNORE_CASE)
