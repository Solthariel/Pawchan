package com.solthariel.pawchan.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A small, metadata-budgeted batch for the explicitly opened attachment picker only. */
internal fun attachmentImagePreloadUrls(attachments: List<MediaAttachmentUi>): List<String> {
    val urls = linkedSetOf<String>()
    var remainingBytes = ATTACHMENT_PRELOAD_TOTAL_BYTES
    for (attachment in attachments) {
        if (urls.size == ATTACHMENT_PRELOAD_MAX_FILES) break
        val bytes = attachment.byteSize ?: 0
        val eligible =
            attachment.viewer == AttachmentViewer.IMAGE && attachment.originalUrl.isNotBlank() &&
                bytes > 0 && bytes <= ATTACHMENT_PRELOAD_FILE_BYTES && bytes <= remainingBytes
        if (eligible && urls.add(attachment.originalUrl)) remainingBytes -= bytes
    }
    return urls.toList()
}

/** Sequential requests, owned and cancelled by the caller; no detached jobs or retries. */
internal suspend fun preloadAttachmentImages(
    attachments: List<MediaAttachmentUi>,
    load: suspend (String) -> Unit,
) {
    val urls = withContext(Dispatchers.Default) { attachmentImagePreloadUrls(attachments) }
    for (url in urls) load(url)
}

private const val ATTACHMENT_PRELOAD_MAX_FILES = 4
private const val ATTACHMENT_PRELOAD_FILE_BYTES = 16L * 1024 * 1024
private const val ATTACHMENT_PRELOAD_TOTAL_BYTES = 32L * 1024 * 1024
