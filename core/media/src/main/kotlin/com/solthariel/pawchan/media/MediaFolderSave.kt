package com.solthariel.pawchan.media

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.webkit.MimeTypeMap
import java.io.FileNotFoundException

/** Files belong to the user-selected directory; failed creations do not prevent other files. */
internal fun createFolderDocuments(
    resolver: ContentResolver,
    tree: Uri,
    attachments: List<MediaAttachmentUi>,
    name: (MediaAttachmentUi) -> String = ::safeMediaFileName,
): List<Pair<MediaAttachmentUi, Uri>> {
    val folder = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
    return createMediaDocuments(
        attachments,
        create = { attachment ->
            val filename = name(attachment)
            val mime = mediaMimeTypeForName(filename, MimeTypeMap.getSingleton()::getMimeTypeFromExtension)
            DocumentsContract.createDocument(resolver, folder, mime, filename)
        },
        discard = { uri -> discardCreatedDocument(resolver, uri) },
    )
}

/** Only removes a document created for this failed download, never an existing user file. */
internal fun discardCreatedDocument(
    resolver: ContentResolver,
    uri: Uri,
) {
    try {
        DocumentsContract.deleteDocument(resolver, uri)
    } catch (_: FileNotFoundException) {
        // It is already absent or unavailable.
    } catch (_: SecurityException) {
        // A revoked grant can also prevent cleanup.
    } catch (_: IllegalArgumentException) {
        // Some providers reject deleting documents.
    } catch (_: UnsupportedOperationException) {
        // Some providers do not support deleting documents.
    }
}
