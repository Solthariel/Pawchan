package com.solthariel.pawchan.composer

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import com.solthariel.pawchan.domain.posting.PostingRules
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import java.lang.ref.Reference

private val nextImport =
    java.util.concurrent.atomic
        .AtomicLong()

internal data class ComposerMediaImportState(
    val receive: (List<Uri>, Any?) -> Unit,
    val error: String?,
)

/** Shared, bounded import for the system picker, clipboard and keyboard content. */
@Composable
internal fun rememberComposerMediaImporter(
    draft: ComposerDraftState,
    rules: PostingRules?,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
    imageLoader: ImageLoader,
): ComposerMediaImportState {
    val latest = rememberUpdatedState(draft)
    val change = rememberUpdatedState(onChange)
    val context = LocalContext.current
    val defaults = LocalComposerPreferences.current.attachments
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    var importing by remember { mutableStateOf(false) }
    val receive: (List<Uri>, Any?) -> Unit = { uris, permission ->
        val current = latest.value
        if (uris.isNotEmpty()) {
            when {
                importing || !enabled || current.inputBlocked -> {
                    error = "Дождитесь завершения текущей операции"
                }

                rules == null -> {
                    error = "Параметры отправки ещё не загружены"
                }

                !canAttachComposerMedia(current, rules) -> {
                    error = "Достигнут лимит вложений"
                }

                else -> {
                    importing = true
                    error = null
                    val token = nextImport.incrementAndGet()
                    change.value(current.withImportId(token, ComposerDraftActivity.LOADING_FILES))
                    scope.launch(start = CoroutineStart.UNDISPATCHED) {
                        var completed = false
                        try {
                            val result = importComposerAttachments(context, uris, current.attachments, rules, defaults)
                            if (result is ComposerAttachmentImport.Success) {
                                prepareComposerThumbnails(
                                    context,
                                    result.files.drop(current.attachments.size),
                                    imageLoader,
                                )
                            }
                            if (latest.value.resetId == current.resetId) {
                                change.value(latest.value.afterImport(result) { error = it }.withImportId(token))
                                completed = true
                            }
                        } finally {
                            val retained = latest.value
                            if (!completed && retained.resetId == current.resetId) {
                                change.value(retained.withImportId(token, ComposerDraftActivity.IDLE))
                            }
                            importing = false
                            // Keep the IME's temporary URI permission alive until asynchronous reading completes.
                            Reference.reachabilityFence(permission)
                        }
                    }
                }
            }
        }
    }
    return ComposerMediaImportState(receive, error)
}

private fun ComposerDraftState.afterImport(
    result: ComposerAttachmentImport,
    failed: (String) -> Unit,
): ComposerDraftState =
    when (result) {
        is ComposerAttachmentImport.Success -> {
            copy(attachments = result.files, activity = ComposerDraftActivity.IDLE)
        }

        is ComposerAttachmentImport.Failure -> {
            failed(result.message ?: "Не удалось прочитать вложение")
            copy(activity = ComposerDraftActivity.IDLE)
        }
    }
