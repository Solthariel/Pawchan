package com.solthariel.pawchan.media

import android.net.Uri

fun interface MediaSaveQueue {
    val defaultDirectory: String? get() = null

    fun suggestedName(attachment: MediaAttachmentUi): String = safeMediaFileName(attachment)

    suspend fun selectDirectory(uri: Uri): Boolean = false

    fun enqueue(
        attachment: MediaAttachmentUi,
        destination: Uri,
    ): Boolean
}
