package com.solthariel.pawchan.arhivach

import com.solthariel.pawchan.core.ui.PostStateMarker
import com.solthariel.pawchan.threads.ThreadListStatusMarkerUi
import com.solthariel.pawchan.threads.ThreadListStatusTone

internal data class ArchiveStatus(
    val postMarker: PostStateMarker,
    val listMarker: ThreadListStatusMarkerUi,
)

internal fun String?.toArchiveStatus(): ArchiveStatus? {
    val raw = this?.trim()?.takeIf(String::isNotEmpty) ?: return null
    val marker = archiveMarker(raw)
    val description = archiveStatusDescription(marker, raw)
    val tone = archiveStatusTone(marker)
    return ArchiveStatus(marker, ThreadListStatusMarkerUi(icon = marker.icon, description = description, tone = tone))
}

private fun archiveMarker(raw: String): PostStateMarker =
    when (raw.lowercase().replace('ё', 'е')) {
        "активный", "active" -> PostStateMarker.ARCHIVE_ACTIVE
        "удален?", "deleted?" -> PostStateMarker.ARCHIVE_MAYBE_DELETED
        "сохранен", "saved" -> PostStateMarker.ARCHIVE_SAVED
        "потерян", "lost" -> PostStateMarker.ARCHIVE_LOST
        "в очереди", "queued", "queue" -> PostStateMarker.ARCHIVE_QUEUED
        else -> PostStateMarker.ARCHIVE_OTHER
    }

private fun archiveStatusDescription(
    marker: PostStateMarker,
    raw: String,
): String =
    when (marker) {
        PostStateMarker.ARCHIVE_ACTIVE -> "Архивач: тред активен и обновляется"
        PostStateMarker.ARCHIVE_MAYBE_DELETED -> "Архивач: исходный тред, возможно, удалён"
        PostStateMarker.ARCHIVE_SAVED -> "Архивач: тред сохранён"
        PostStateMarker.ARCHIVE_LOST -> "Архивач: тред помечен как потерянный"
        PostStateMarker.ARCHIVE_QUEUED -> "Архивач: тред ожидает обработки"
        else -> "Статус Архивача: $raw"
    }

private fun archiveStatusTone(marker: PostStateMarker): ThreadListStatusTone =
    when (marker) {
        PostStateMarker.ARCHIVE_ACTIVE -> ThreadListStatusTone.PRIMARY
        PostStateMarker.ARCHIVE_MAYBE_DELETED -> ThreadListStatusTone.TERTIARY
        PostStateMarker.ARCHIVE_SAVED -> ThreadListStatusTone.SECONDARY
        PostStateMarker.ARCHIVE_LOST -> ThreadListStatusTone.ERROR
        else -> ThreadListStatusTone.MUTED
    }
