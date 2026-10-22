package com.solthariel.pawchan.copyfiles

internal data class CopyDocument(
    val uri: String,
    val name: String,
    val mime: String,
    val modified: Long,
    val bytes: Long,
)

internal enum class CopyFileKind(
    val folder: String,
    val mime: String,
    val extension: String,
) {
    BACKUP("Backups", "application/octet-stream", ".pawbackup"),
    THREAD("Archive", "application/zip", ".zip"),
}

internal fun copyDocumentMatches(
    name: String,
    kind: CopyFileKind,
): Boolean = name.endsWith(kind.extension, ignoreCase = true)

internal fun sortedCopyDocuments(files: List<CopyDocument>): List<CopyDocument> =
    files.sortedWith(compareByDescending<CopyDocument> { it.modified }.thenBy { it.name })
