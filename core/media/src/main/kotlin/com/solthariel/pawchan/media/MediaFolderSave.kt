package com.solthariel.pawchan.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import java.io.FileNotFoundException

/** Файлы в выбранной пользователем папке; не созданные (нет места, нет доступа) пропускаются. */
internal fun createFolderDocuments(
    resolver: ContentResolver,
    tree: Uri,
    attachments: List<MediaAttachmentUi>,
    mimeType: String,
    name: (MediaAttachmentUi) -> String = ::safeMediaFileName,
): List<Pair<MediaAttachmentUi, Uri>> {
    val folder = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    return attachments.mapNotNull { attachment ->
        try {
            DocumentsContract
                .createDocument(resolver, folder, mimeType, name(attachment))
                ?.let { attachment to it }
        } catch (_: FileNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }
    }
}
