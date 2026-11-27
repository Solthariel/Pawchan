package com.solthariel.pawchan.infrastructure.thread.export

import android.content.Context
import android.net.Uri
import com.solthariel.pawchan.thread.CapturedThreadShot
import com.solthariel.pawchan.thread.ThreadExportDocument
import com.solthariel.pawchan.thread.ThreadExportTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Borrowed context; the calling screen owns cancellation, this target owns its stream. */
internal class AppThreadExportTarget(
    private val context: Context,
    private val uri: Uri,
) : ThreadExportTarget {
    override suspend fun saveHtml(document: ThreadExportDocument) {
        val text = withContext(Dispatchers.Default) { threadHtml(document) }
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use {
                it.write(text.toByteArray())
                it.flush()
            } ?: throw IOException("Нет доступа к файлу")
        }
    }

    override suspend fun saveShot(image: CapturedThreadShot) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use {
                image.writePng(it)
            } ?: throw IOException("Нет доступа к файлу")
        }
    }
}
