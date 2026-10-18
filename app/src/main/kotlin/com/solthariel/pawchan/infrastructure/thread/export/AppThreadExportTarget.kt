package com.solthariel.pawchan.infrastructure.thread.export

import android.content.Context
import android.net.Uri
import coil3.ImageLoader
import com.solthariel.pawchan.thread.ThreadExportDocument
import com.solthariel.pawchan.thread.ThreadExportPost
import com.solthariel.pawchan.thread.ThreadExportTarget
import com.solthariel.pawchan.thread.ThreadShotStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Borrowed context/loader; the calling screen owns cancellation, this target owns its stream. */
internal class AppThreadExportTarget(
    private val context: Context,
    private val loader: ImageLoader,
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

    override suspend fun saveShot(
        posts: List<ThreadExportPost>,
        style: ThreadShotStyle,
    ) {
        withContext(Dispatchers.IO) {
            context.contentResolver.openOutputStream(uri)?.use {
                renderThreadShot(context, loader, posts, style, it)
            } ?: throw IOException("Нет доступа к файлу")
        }
    }
}
