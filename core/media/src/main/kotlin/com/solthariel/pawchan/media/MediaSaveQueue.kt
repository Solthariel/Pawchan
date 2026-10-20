package com.solthariel.pawchan.media

import android.net.Uri

fun interface MediaSaveQueue {
    val defaultDirectory: String? get() = null

    fun suggestedName(attachment: MediaAttachmentUi): String = safeMediaFileName(attachment)

    suspend fun selectDirectory(uri: Uri): Boolean = false

    /** Holds a selected tree while documents are being created and enqueued. */
    fun useDirectory(directory: Uri): AutoCloseable = AutoCloseable {}

    fun enqueue(
        attachment: MediaAttachmentUi,
        destination: Uri,
    ): Boolean
}
