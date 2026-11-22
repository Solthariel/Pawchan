package com.solthariel.pawchan.downloads

import androidx.work.WorkInfo

internal fun resolvedDownloadRecords(
    records: List<DownloadRecord>,
    works: List<WorkInfo>?,
): List<DownloadRecord> = resolveDownloadStates(records, works?.associate { it.id.toString() to it.state })

internal fun resolveDownloadStates(
    records: List<DownloadRecord>,
    statesById: Map<String, WorkInfo.State>?,
): List<DownloadRecord> =
    records.map { record ->
        record.copy(status = resolvedStatus(record, statesById?.get(record.id), statesById != null))
    }

internal data class PreparedDownloads(
    val source: List<DownloadRecord>,
    val resolved: List<DownloadRecord>,
    val visible: List<DownloadRecord>,
)

internal fun prepareDownloads(
    records: List<DownloadRecord>,
    works: List<WorkInfo>?,
    query: String?,
): PreparedDownloads {
    val resolved = resolvedDownloadRecords(records, works)
    val visible = resolved.filter { query.isNullOrBlank() || it.name.contains(query, ignoreCase = true) }
    return PreparedDownloads(records, resolved, visible)
}
