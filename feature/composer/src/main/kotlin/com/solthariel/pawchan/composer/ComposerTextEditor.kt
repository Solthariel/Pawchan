package com.solthariel.pawchan.composer

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyDraft
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerTextEditor(
    draft: ComposerDraftState,
    rules: PostingRules?,
    enabled: Boolean,
    actions: ComposerFieldActions,
    onReceive: (List<Uri>, Any?) -> Unit = { _, _ -> },
) {
    val comment = rememberTextFieldState(draft.comment)
    val latest = rememberUpdatedState(draft)
    val change = rememberUpdatedState(actions.onChange)
    val commentFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(draft.comment) {
        if (comment.text.toString() != draft.comment) {
            comment.edit {
                replace(0, length, draft.comment)
                selection = TextRange(draft.comment.length)
            }
        }
    }
    LaunchedEffect(comment) {
        snapshotFlow { comment.text.toString() }.collect { text ->
            if (latest.value.comment != text) change.value(latest.value.copy(comment = text))
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ComposerCommentField(
            comment,
            ComposerLimits(
                maxFileBytes = rules?.maxTotalFileBytes ?: DEFAULT_MAX_FILE_BYTES,
                remainingUploadBytes = composerUploadBudget(draft.attachments, rules).remaining,
                maxFiles = rules?.maxFiles ?: DEFAULT_MAX_FILES,
                maxCommentLength = rules?.maxCommentLength ?: ReplyDraft.MAX_COMMENT_LENGTH,
            ),
            enabled && !draft.inputBlocked,
            commentFocus,
            Modifier.composerMediaReceiver(onReceive),
            preserveAppearance = draft.filesLoading,
        )
        ComposerFormattingToolbar(enabled, draft.inputBlocked, actions.onClearAll) { markup ->
            val editRange = CommentEdit(comment.text.toString(), comment.selection.start, comment.selection.end)
            val edit = applyCommentMarkup(editRange, markup)
            comment.edit {
                replace(0, length, edit.text)
                selection = TextRange(edit.selectionStart, edit.selectionEnd)
            }
            change.value(latest.value.copy(comment = edit.text))
            // после кнопки разметки курсор уже между тегами — сразу печатаем.
            commentFocus.requestFocus()
            keyboard?.show()
        }
    }
}

@Composable
internal fun ComposerCommentField(
    value: TextFieldState,
    limits: ComposerLimits,
    enabled: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    preserveAppearance: Boolean = false,
) {
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.bodyLarge
    // Material3 1.4.0 centers the inner editor in a 24.dp minimum-height box.
    // Below fontScale=1 a 24.sp line is shorter, so its extra top inset disappears
    // on Enter. Match that minimum without changing the font size or stored text.
    val lineHeight =
        with(density) {
            maxOf(textStyle.lineHeight.toPx(), COMMENT_MIN_LINE_HEIGHT.toPx()).toSp()
        }
    OutlinedTextField(
        state = value,
        modifier = modifier.fillMaxWidth().heightIn(min = 120.dp).focusRequester(focusRequester),
        textStyle = textStyle.copy(lineHeight = lineHeight),
        label = { Text(stringResource(R.string.reply_comment)) },
        supportingText = {
            Text(
                text = composerLimitsText(limits, value.text.length),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.End,
            )
        },
        isError = value.text.length > limits.maxCommentLength,
        enabled = enabled,
        colors = composerCommentColors(preserveAppearance),
    )
}

internal data class ComposerLimits(
    val maxFileBytes: Int,
    val maxFiles: Int,
    val maxCommentLength: Int,
    val remainingUploadBytes: Long? = maxFileBytes.toLong(),
)

internal fun formatComposerMegabytes(bytes: Int): String {
    val megabytes = bytes.toDouble() / BYTES_PER_MEGABYTE
    return if (megabytes % 1.0 == 0.0) megabytes.toInt().toString() else "%.1f".format(megabytes)
}

private const val DEFAULT_MAX_FILES = 4
private const val DEFAULT_MAX_FILE_BYTES = 40 * 1024 * 1024
private const val BYTES_PER_MEGABYTE = 1024 * 1024

private val COMMENT_MIN_LINE_HEIGHT = 24.dp
