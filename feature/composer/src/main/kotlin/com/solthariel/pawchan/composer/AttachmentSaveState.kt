package com.solthariel.pawchan.composer

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.solthariel.pawchan.domain.posting.ReplyAttachment
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Viewer-owned work: cancellation never publishes a partial export or the original. */
internal class AttachmentSaveState(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
    private val prepare: (ComposerAttachment) -> ReplyAttachment = ::prepareAttachment,
) {
    var noticeVersion by mutableStateOf(0L)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    fun save(
        file: ComposerAttachment,
        onSaved: (ComposerAttachment) -> Unit,
    ) {
        if (busy) return
        noticeVersion++
        if (!file.validName) {
            error = "Недопустимое имя файла"
            return
        }
        busy = true
        error = null
        scope.launch {
            try {
                val result =
                    withContext(dispatcher) {
                        if (file.preparedUpload != null) file else prepareUploadCache(file, prepare)
                    }
                ensureActive()
                if (result.preparedUpload == null) {
                    error = result.prepared?.failure ?: ATTACHMENT_PROCESSING_ERROR
                } else {
                    onSaved(result)
                }
            } finally {
                busy = false
            }
        }
    }
}
