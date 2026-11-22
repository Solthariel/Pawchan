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
                    attachments.count { attachment ->
                        queue.enqueueInFolder(attachment, tree) ?: createFolderDocuments(
                            resolver,
                            tree,
                            listOf(attachment),
                            queue::suggestedName,
                        ).any { (_, destination) ->
                            val accepted = queue.enqueue(attachment, destination)
                            if (!accepted) discardCreatedDocument(resolver, destination)
                            accepted
                        }
                    }
                }
            } catch (_: IllegalArgumentException) {
                0
            }
        if (queued == attachments.size && queued > 0) MediaSaveStatus.QueuedMany(queued) else MediaSaveStatus.Failed
    }
