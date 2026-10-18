package com.solthariel.pawchan.composer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.solthariel.pawchan.core.ui.UiIcon
import com.solthariel.pawchan.core.ui.UiIconKind
import com.solthariel.pawchan.domain.posting.PostingRules
import com.solthariel.pawchan.domain.settings.ComposerField
import com.solthariel.pawchan.feature.composer.R

internal data class ComposerScreenState(
    val draft: ComposerDraftState,
    val rules: PostingRules?,
    val captcha: ComposerCaptchaState,
    val status: ComposerScreenStatus,
)

internal data class ComposerScreenStatus(
    val progress: ComposerProgressState,
    val failure: ComposerFailureState,
    val canSubmit: Boolean,
)

internal data class ComposerScreenActions(
    val fields: ComposerFieldActions,
    val captcha: NewThreadCaptchaActions,
    val onSubmit: () -> Unit,
    val onClose: () -> Unit,
)

@Composable
internal fun ComposerEditorScreen(
    title: String,
    newThread: Boolean,
    state: ComposerScreenState,
    imageLoader: ImageLoader,
    actions: ComposerScreenActions,
) {
    BackHandler(onBack = actions.onClose)
    val enabled = !state.status.progress.submitting && !state.status.progress.sent
    val picker =
        key(state.draft.resetId) {
            rememberComposerAttachmentPicker(state.draft, state.rules, enabled, actions.fields.onChange)
        }
    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        ComposerScreenHeader(
            title,
            state,
            enabled,
            actions.onClose,
            picker.onAttach,
            actions.fields.onChange,
        )
        ComposerScreenBody(
            state,
            imageLoader,
            actions,
            newThread,
            picker,
            Modifier.weight(1f),
        )
    }
}

@Composable
private fun ComposerScreenBody(
    state: ComposerScreenState,
    imageLoader: ImageLoader,
    actions: ComposerScreenActions,
    newThread: Boolean,
    picker: ComposerAttachmentPickerState,
    modifier: Modifier,
) {
    Column(
        modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState(), overscrollEffect = null)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ComposerEditorFields(state, imageLoader, actions, newThread, picker)
            ComposerStatusLine(state.captcha, state.status, actions.captcha.onReloadCaptcha, imageLoader)
        }
        ComposerCaptcha(state.captcha, state.status.progress, imageLoader, actions.captcha)
        ComposerSubmitButton(state, newThread, actions.onSubmit)
    }
}

@Composable
private fun ComposerEditorFields(
    state: ComposerScreenState,
    imageLoader: ImageLoader,
    actions: ComposerScreenActions,
    newThread: Boolean,
    picker: ComposerAttachmentPickerState,
) {
    val enabled = !state.status.progress.submitting && !state.status.progress.sent
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        key(state.draft.resetId) {
            ComposerTextEditor(
                state.draft,
                state.rules,
                enabled,
                newThread,
                actions.fields,
            )
        }
        key(state.draft.resetId) {
            ComposerMediaSection(state.draft, imageLoader, enabled, picker.error, actions.fields.onChange)
        }
        ComposerAttachmentRequirement(state.draft, newThread)
    }
}

@Composable
private fun ComposerAttachmentRequirement(
    draft: ComposerDraftState,
    newThread: Boolean,
) {
    if (newThread && draft.attachments.isEmpty()) {
        Text(
            stringResource(R.string.new_thread_attachment_required),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ComposerSubmitButton(
    state: ComposerScreenState,
    newThread: Boolean,
    onSubmit: () -> Unit,
) {
    Button(
        onClick = onSubmit,
        enabled = state.status.canSubmit,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (state.status.progress.submitting) {
            CircularProgressIndicator()
        } else {
            Text(stringResource(if (newThread) R.string.new_thread_create else R.string.reply_send))
        }
    }
}

@Composable
private fun ComposerScreenHeader(
    title: String,
    state: ComposerScreenState,
    enabled: Boolean,
    onClose: () -> Unit,
    onAttach: () -> Unit,
    onChange: (ComposerDraftState) -> Unit,
) {
    val close = stringResource(R.string.board_dialog_close)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onClose, modifier = Modifier.semantics { contentDescription = close }) {
            UiIcon(UiIconKind.BACK, MaterialTheme.colorScheme.onSurface, Modifier.size(24.dp))
        }
        Text(title, modifier = Modifier.weight(1f), maxLines = 2, style = MaterialTheme.typography.titleMedium)
        run {
            ComposerIconSelector(state.draft, state.rules, enabled, onChange, compact = true)
        }
        ComposerPostingOptions(state.draft, state.rules, enabled && !state.draft.filesLoading, onChange)
        if (canAttachComposerMedia(state.draft, state.rules)) ComposerAttachButton(onAttach)
    }
}

internal fun canAttachComposerMedia(
    draft: ComposerDraftState,
    rules: PostingRules?,
): Boolean = draft.attachments.size < (rules?.maxFiles ?: DEFAULT_POST_FILES)

private const val DEFAULT_POST_FILES = 4
