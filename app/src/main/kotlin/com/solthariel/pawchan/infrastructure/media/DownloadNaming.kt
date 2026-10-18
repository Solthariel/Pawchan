package com.solthariel.pawchan.infrastructure.media

import com.solthariel.pawchan.domain.settings.DownloadNaming
import com.solthariel.pawchan.media.MediaAttachmentUi

internal fun downloadName(
    attachment: MediaAttachmentUi,
    mode: DownloadNaming,
): String {
    val original =
        attachment.fileName
            .substringAfterLast('/')
            .substringAfterLast('\\')
            .trim()
            .ifBlank { "media" }
    val prefix = attachment.source.downloadPrefix ?: attachment.source.ownerPostId?.toString() ?: attachment.id
    val selected = if (mode == DownloadNaming.DETAILED) "${prefix}_$original" else original
    val safe = sanitizedDownloadName(selected)
    val extension = safe.substringAfterLast('.', "").takeIf { it.length in 1..MAX_EXTENSION_LENGTH }
    return if (safe.length <= MAX_NAME) {
        safe
    } else {
        safe.take(MAX_NAME - (extension?.length ?: 0) - 1) +
            (extension?.let { ".$it" } ?: "")
    }
}

private const val MAX_NAME = 180

private const val MAX_EXTENSION_LENGTH = 10

private fun sanitizedDownloadName(value: String): String {
    val clean = value.map { if (it.isISOControl() || it in "/\\:*?\"<>|") '_' else it }.joinToString("")
    return clean.takeUnless { it == "." || it == ".." } ?: "media"
}
