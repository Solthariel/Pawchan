package com.solthariel.pawchan.composer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.feature.composer.R
import kotlinx.coroutines.launch

internal data class ComposerAttachmentPickerState(
    val onAttach: () -> Unit,
    val error: String?,
)

@Composable
internal fun rememberComposerAttachmentPicker(
    draft: ComposerDraftState,
    rules: PostingRules?,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
): ComposerAttachmentPickerState {
    val latest = rememberUpdatedState(draft)
    val pickerHost = LocalComposerPickerHost.current
    DisposableEffect(pickerHost) { onDispose { pickerHost.cancel() } }
    val defaults = LocalComposerPreferences.current.attachments
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var error by remember { mutableStateOf<String?>(null) }
    val readFailed = stringResource(R.string.attachment_read_failed)
    val busy = stringResource(R.string.attachment_busy)
    val unavailable = stringResource(R.string.attachment_rules_unavailable)
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            pickerHost.returned()
            val mayImport = enabled && !draft.filesLoading && canAttachComposerMedia(draft, rules)
            if (uris.isNotEmpty() && rules != null && mayImport) {
                error = null
                onChange(draft.copy(filesLoading = true))
                scope.launch {
                    when (val result = importComposerAttachments(context, uris, draft.attachments, rules, defaults)) {
                        is ComposerAttachmentImport.Success -> {
                            if (latest.value.resetId == draft.resetId) {
                                onChange(latest.value.copy(attachments = result.files, filesLoading = false))
                            }
                        }

                        is ComposerAttachmentImport.Failure -> {
                            error = result.message ?: readFailed
                            if (latest.value.resetId == draft.resetId) {
                                onChange(latest.value.copy(filesLoading = false))
                            }
                        }
                    }
                }
            }
        }
    return ComposerAttachmentPickerState(
        onAttach = {
            when {
                !enabled || draft.filesLoading -> {
                    error = busy
                }

                rules == null -> {
                    error = unavailable
                }

                canAttachComposerMedia(draft, rules) -> {
                    launchAttachmentPicker(
                        pickerHost,
                        { launcher.launch(ATTACHMENT_MIME_TYPES) },
                        { error = readFailed },
                    )
                }
            }
        },
        error = error,
    )
}

private val ATTACHMENT_MIME_TYPES = arrayOf("image/*", "video/webm", "video/mp4")

private fun launchAttachmentPicker(
    host: ComposerPickerHost,
    launch: () -> Unit,
    failed: () -> Unit,
) {
    host.begin()
    try {
        launch()
    } catch (_: android.content.ActivityNotFoundException) {
        host.cancel()
        failed()
    } catch (_: IllegalStateException) {
        host.cancel()
        failed()
    }
}

internal fun canAttachComposerMedia(
    draft: ComposerDraftState,
    rules: PostingRules?,
): Boolean = draft.attachments.size < (rules?.maxFiles ?: DEFAULT_POST_FILES)

private const val DEFAULT_POST_FILES = 4
