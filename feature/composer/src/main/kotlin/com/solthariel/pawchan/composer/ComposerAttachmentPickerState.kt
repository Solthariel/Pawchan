package com.solthariel.pawchan.composer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.feature.composer.R

internal data class ComposerAttachmentPickerState(
    val onAttach: () -> Unit,
    val error: String?,
    val onReceive: (List<android.net.Uri>, Any?) -> Unit = { _, _ -> },
)

@Composable
internal fun rememberComposerAttachmentPicker(
    draft: ComposerDraftState,
    rules: PostingRules?,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
    imageLoader: ImageLoader,
): ComposerAttachmentPickerState {
    val pickerHost = LocalComposerPickerHost.current
    DisposableEffect(pickerHost) { onDispose { pickerHost.cancel() } }
    val importer = rememberComposerMediaImporter(draft, rules, enabled, onChange, imageLoader)
    var error by remember { mutableStateOf<String?>(null) }
    val readFailed = stringResource(R.string.attachment_read_failed)
    val busy = stringResource(R.string.attachment_busy)
    val unavailable = stringResource(R.string.attachment_rules_unavailable)
    val launcher =
        rememberLauncherForActivityResult(ComposerGetMultipleContents()) { uris ->
            pickerHost.returned()
            importer.receive(uris, null)
        }

    return ComposerAttachmentPickerState(
        onAttach = {
            when {
                !enabled || draft.inputBlocked -> {
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
        error = error ?: importer.error,
        onReceive = importer.receive,
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
