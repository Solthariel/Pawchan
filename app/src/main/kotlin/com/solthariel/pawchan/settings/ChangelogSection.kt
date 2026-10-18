package com.solthariel.pawchan.settings

internal data class ChangelogSection(
    val title: String,
    val entries: List<String>,
)

/** Prepared off Main; Markdown list markers and internal version headers are not UI text. */
internal fun parseChangelog(source: String): List<ChangelogSection> {
    val result = mutableListOf<ChangelogSection>()
    var title = "Изменения"
    var entries = mutableListOf<String>()
    source.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.isBlank() || line.startsWith("Версия ") -> {
                Unit
            }

            line.startsWith("* ") || line.startsWith("- ") || line.startsWith("• ") -> {
                line
                    .drop(2)
                    .trim()
                    .takeIf(String::isNotEmpty)
                    ?.let(entries::add)
            }

            else -> {
                if (entries.isNotEmpty()) result.add(ChangelogSection(title, entries.toList()))
                title = line.trimStart('#', ' ').removeSuffix(":")
                entries = mutableListOf()
            }
        }
    }
    if (entries.isNotEmpty()) result.add(ChangelogSection(title, entries.toList()))
    return result
}
