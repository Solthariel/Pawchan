package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.domain.posting.NewThreadDraft
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.posting.ReplyDraft
import com.solthariel.pawchan.domain.settings.ComposerField
import com.solthariel.pawchan.feature.composer.R

@Composable
internal fun ComposerTextEditor(
    draft: ComposerDraftState,
    rules: PostingRules?,
    enabled: Boolean,
    showSubject: Boolean,
    actions: ComposerFieldActions,
) {
    val hidden = LocalComposerPreferences.current.hiddenFields
    var comment by remember { mutableStateOf(TextFieldValue(draft.comment)) }
    val commentFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(draft.comment) {
        if (comment.text != draft.comment) comment = TextFieldValue(draft.comment)
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ComposerIdentityFields(
            draft,
            rules,
            enabled && !draft.filesLoading,
            actions.onChange,
            showIconSelector = false,
        )
        if (showSubject && rules?.subjectEnabled == true &&
            ComposerField.SUBJECT !in hidden
        ) {
            ComposerSubjectField(draft, enabled && !draft.filesLoading, actions.onChange)
        }
        if (showSubject && rules?.tagsEnabled == true &&
            ComposerField.TAG !in hidden
        ) {
            ComposerTagField(draft, enabled && !draft.filesLoading, actions.onChange)
        }
        ComposerCommentField(
            comment,
            ComposerLimits(
                maxFileBytes = rules?.maxTotalFileBytes ?: DEFAULT_MAX_FILE_BYTES,
                remainingUploadBytes = composerUploadBudget(draft.attachments, rules).remaining,
                maxFiles = rules?.maxFiles ?: DEFAULT_MAX_FILES,
                maxCommentLength = rules?.maxCommentLength ?: ReplyDraft.MAX_COMMENT_LENGTH,
            ),
            enabled && !draft.filesLoading,
            commentFocus,
        ) { value ->
            comment = value
            actions.onChange(draft.copy(comment = value.text))
        }
        ComposerFormattingToolbar(enabled, draft.filesLoading, actions.onClearAll) { markup ->
            val selection = CommentEdit(comment.text, comment.selection.start, comment.selection.end)
            val edit = applyCommentMarkup(selection, markup)
            comment = TextFieldValue(edit.text, TextRange(edit.selectionStart, edit.selectionEnd))
            actions.onChange(draft.copy(comment = edit.text))
            // Как в Dashchan: после кнопки разметки курсор уже между тегами — сразу печатаем.
            commentFocus.requestFocus()
            keyboard?.show()
        }
    }
}

@Composable
private fun ComposerTagField(
    draft: ComposerDraftState,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
) {
    OutlinedTextField(
        value = draft.tag,
        onValueChange = { value ->
            if (value.length <= NewThreadDraft.MAX_TAG_LENGTH) {
                onChange(draft.copy(options = draft.options.copy(tag = value)))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.composer_tag)) },
        supportingText = {
            Text(stringResource(R.string.reply_length, draft.tag.length, NewThreadDraft.MAX_TAG_LENGTH))
        },
        singleLine = true,
        enabled = enabled,
    )
}

@Composable
private fun ComposerSubjectField(
    draft: ComposerDraftState,
    enabled: Boolean,
    onChange: (ComposerDraftState) -> Unit,
) {
    OutlinedTextField(
        value = draft.subject,
        onValueChange = { onChange(draft.copy(subject = it)) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(R.string.new_thread_subject)) },
        supportingText = {
            Text(stringResource(R.string.reply_length, draft.subject.length, NewThreadDraft.MAX_SUBJECT_LENGTH))
        },
        isError = draft.subject.length > NewThreadDraft.MAX_SUBJECT_LENGTH,
        singleLine = true,
        enabled = enabled,
    )
}

@Composable
internal fun ComposerCommentField(
    value: TextFieldValue,
    limits: ComposerLimits,
    enabled: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
    onChange: (TextFieldValue) -> Unit,
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
        value = value,
        onValueChange = onChange,
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
