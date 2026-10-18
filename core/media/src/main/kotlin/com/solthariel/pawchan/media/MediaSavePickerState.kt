package com.solthariel.pawchan.media

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.IOException

internal class MediaSavePickerState(
    private val queue: MediaSaveQueue,
    private val context: Context,
    private val scope: CoroutineScope,
) {
    var status by mutableStateOf<MediaSaveStatus>(MediaSaveStatus.Idle)
    var needsDirectory by mutableStateOf(false)
        private set
    private var pending = emptyList<MediaAttachmentUi>()

    fun pickerStarted() {
        needsDirectory = false
    }

    fun directoryPicked(uri: Uri?) {
        val files = pending
        pending = emptyList()
        if (uri == null) return
        scope.launch {
            if (queue.selectDirectory(uri)) requestAll(files) else status = MediaSaveStatus.Failed
        }
    }

    fun request(attachment: MediaAttachmentUi) = requestAll(listOf(attachment))

    fun requestAll(attachments: List<MediaAttachmentUi>) {
        if (attachments.isEmpty()) return
        val tree = queue.defaultDirectory
        if (tree == null) {
            pending = (pending + attachments).distinctBy { it.originalUrl }
            needsDirectory = true
            return
        }
        scope.launch {
            status =
                try {
                    saveToFolder(context.contentResolver, tree.toUri(), attachments, queue)
                } catch (_: IOException) {
                    MediaSaveStatus.Failed
                } catch (_: SecurityException) {
                    pending = attachments
                    needsDirectory = true
                    MediaSaveStatus.Idle
                } catch (_: IllegalArgumentException) {
                    MediaSaveStatus.Failed
                }
        }
    }
}
