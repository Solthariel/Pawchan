package com.solthariel.pawchan.library

import com.solthariel.pawchan.domain.boards.ArchivedThreadSummary
import com.solthariel.pawchan.domain.model.ThreadAddress
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

internal data class ArchiveThreadPresentation(
    val address: ThreadAddress,
    val subject: String,
    val metadata: String,
)

internal fun presentArchiveThread(
    thread: ArchivedThreadSummary,
    zone: ZoneId = ZoneId.systemDefault(),
): ArchiveThreadPresentation {
    val created =
        thread.timestampSeconds?.let {
            DateTimeFormatter.ofPattern("dd.MM.yy HH:mm").withZone(zone).format(Instant.ofEpochSecond(it))
        }
    val metadata =
        listOfNotNull(
            "№${thread.address.thread.value}",
            created,
            thread.archiveDate?.let {
                "Дата в архиве: $it"
            },
        ).joinToString("   ")
    return ArchiveThreadPresentation(thread.address, thread.subject, metadata)
}
