package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.produceState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.IOException

/** Debounce edits and serialize bitmap work. Never show the source weight as the output weight. */
@Composable
internal fun rememberReencodingSize(file: ComposerAttachment) =
    key(file.id, file.options) {
        produceState(ReencodingPreviewSize()) {
            delay(SIZE_PREVIEW_DELAY_MS)
            value =
                withContext(sizePreviewDispatcher) {
                    val size = encodedAttachmentSize(file)
                    ensureActive()
                    ReencodingPreviewSize(size, calculating = false)
                }
        }
    }

internal data class ReencodingPreviewSize(
    val bytes: Long? = null,
    val calculating: Boolean = true,
)

internal fun encodedAttachmentSize(file: ComposerAttachment): Long? =
    try {
        prepareAttachment(file).content.size.toLong()
    } catch (_: IllegalArgumentException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: IOException) {
        null
    }

private val sizePreviewDispatcher = Dispatchers.Default.limitedParallelism(1)
private const val SIZE_PREVIEW_DELAY_MS = 350L
