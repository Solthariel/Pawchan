package com.solthariel.pawchan.data.network

import com.solthariel.pawchan.domain.archive.ArchiveMediaDetails
import com.solthariel.pawchan.domain.model.MediaDimensions
import org.jsoup.nodes.Element

internal fun archiveMediaDetails(block: Element): ArchiveMediaDetails? {
    val title =
        block
            .attr("title")
            .lineSequence()
            .firstOrNull()
            .orEmpty()
    val match = ARCHIVE_MEDIA_METADATA.find(title) ?: return null
    val sizeLabel = "${match.groupValues[1]} ${match.groupValues[2].uppercase()}"
    val dimensions =
        MediaDimensions.create(
            match.groupValues[WIDTH_GROUP].toIntOrNull(),
            match.groupValues[HEIGHT_GROUP].toIntOrNull(),
        )
    return ArchiveMediaDetails(sizeLabel, dimensions)
}

private val ARCHIVE_MEDIA_METADATA =
    Regex(
        """^\s*(\d+(?:[.,]\d+)?)\s*([КМГ]б|[KMG]B|байт|bytes?)""" +
            """\s*,\s*(\d+)\s*[xх×]\s*(\d+)""",
        RegexOption.IGNORE_CASE,
    )

private const val WIDTH_GROUP = 3
private const val HEIGHT_GROUP = 4
