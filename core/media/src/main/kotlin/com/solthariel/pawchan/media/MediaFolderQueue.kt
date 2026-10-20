package com.solthariel.pawchan.media

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun saveToFolder(
    resolver: ContentResolver,
    tree: Uri,
    attachments: List<MediaAttachmentUi>,
    queue: MediaSaveQueue,
): MediaSaveStatus =
    withContext(Dispatchers.IO) {
        val queued =
            try {
                queue.useDirectory(tree).use {
                    createFolderDocuments(resolver, tree, attachments, "application/octet-stream", queue::suggestedName)
                        .count { (attachment, destination) -> queue.enqueue(attachment, destination) }
                }
            } catch (_: IllegalArgumentException) {
                0
            }
        if (queued == attachments.size && queued > 0) MediaSaveStatus.QueuedMany(queued) else MediaSaveStatus.Failed
    }
