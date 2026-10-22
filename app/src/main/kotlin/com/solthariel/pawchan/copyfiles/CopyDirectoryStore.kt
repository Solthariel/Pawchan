package com.solthariel.pawchan.copyfiles

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.OutputStream

/** Only this adapter touches document providers. The caller owns the scope and chooses the tree. */
internal class CopyDirectoryStore(
    private val resolver: ContentResolver,
    private val lease: (String) -> AutoCloseable,
) : CopyFileStore {
    private val mutex = Mutex()

    override suspend fun write(
        tree: String,
        kind: CopyFileKind,
        name: String,
        writer: suspend (OutputStream) -> Unit,
    ): CopyDocument =
        withContext(Dispatchers.IO) {
            require(name.isNotBlank() && '/' !in name && '\\' !in name)
            mutex.withLock {
                lease(tree).use {
                    val root = directory(tree, kind.folder, create = true) ?: throw IOException("Нет доступа к папке")
                    val uri =
                        DocumentsContract.createDocument(resolver, root, kind.mime, name)
                            ?: throw IOException("Не удалось создать файл")
                    var committed = false
                    try {
                        val stream = resolver.openOutputStream(uri, "wt") ?: throw IOException("Нет доступа к файлу")
                        stream.use { writer(it) }
                        currentCoroutineContext().ensureActive()
                        committed = true
                        CopyDocument(uri.toString(), name, kind.mime, System.currentTimeMillis(), -1)
                    } finally {
                        if (!committed) withContext(NonCancellable) { removePartial(uri) }
                    }
                }
            }
        }

    override suspend fun list(
        tree: String,
        kind: CopyFileKind,
    ): List<CopyDocument> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                lease(tree).use {
                    val uri = tree.toUri()
                    val selected =
                        DocumentsContract.buildDocumentUriUsingTree(
                            uri,
                            DocumentsContract.getTreeDocumentId(uri),
                        )
                    val root = directory(tree, kind.folder, create = false)
                    val files = children(selected) + if (root == null) emptyList() else children(root)
                    sortedCopyDocuments(
                        files.filter {
                            it.mime != DocumentsContract.Document.MIME_TYPE_DIR && copyDocumentMatches(it.name, kind)
                        },
                    )
                }
            }
        }

    private fun directory(
        tree: String,
        name: String,
        create: Boolean,
    ): Uri? {
        val uri = tree.toUri()
        val root = DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri))
        val existing = children(root).firstOrNull { it.name == name }
        if (existing != null) {
            if (existing.mime != DocumentsContract.Document.MIME_TYPE_DIR) throw IOException("Вместо папки найден файл")
            return existing.uri.toUri()
        }
        return if (create) {
            DocumentsContract.createDocument(resolver, root, DocumentsContract.Document.MIME_TYPE_DIR, name)
        } else {
            null
        }
    }

    private fun children(parent: Uri): List<CopyDocument> {
        val children =
            DocumentsContract.buildChildDocumentsUriUsingTree(
                parent,
                DocumentsContract.getDocumentId(parent),
            )
        val columns =
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                DocumentsContract.Document.COLUMN_SIZE,
            )
        val cursor =
            resolver.query(children, columns, null, null, null)
                ?: throw IOException("Не удалось прочитать папку")
        return cursor.use {
            buildList {
                while (it.moveToNext()) {
                    val uri = DocumentsContract.buildDocumentUriUsingTree(parent, it.getString(ID_COLUMN))
                    add(
                        CopyDocument(
                            uri.toString(),
                            it.getString(NAME_COLUMN).orEmpty(),
                            it.getString(MIME_COLUMN).orEmpty(),
                            it.getLong(MODIFIED_COLUMN),
                            it.getLong(SIZE_COLUMN),
                        ),
                    )
                }
            }
        }
    }

    private fun removePartial(uri: Uri) {
        try {
            DocumentsContract.deleteDocument(resolver, uri)
        } catch (_: IOException) {
            // Keep the original write error; a provider may already have removed the incomplete document.
        } catch (_: SecurityException) {
            // A revoked grant cannot be used even for cleanup.
        }
    }
}

private const val ID_COLUMN = 0
private const val NAME_COLUMN = 1
private const val MIME_COLUMN = 2
private const val MODIFIED_COLUMN = 3
private const val SIZE_COLUMN = 4
