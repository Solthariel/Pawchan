package com.solthariel.pawchan.composer

import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.solthariel.pawchan.core.ui.LocalBottomOverlayInset
import com.solthariel.pawchan.core.ui.focusedWindowBottomPadding
import com.solthariel.pawchan.domain.posting.PostingRules

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
    val uploadBudget: ComposerUploadBudget? = null,
    val canRequestSubmit: Boolean = canSubmit,
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
    val enabled = !state.status.progress.submitting && !state.status.progress.sent && !state.draft.inputBlocked
    val picker =
        key(state.draft.resetId) {
            rememberComposerAttachmentPicker(
                state.draft,
                state.rules,
                enabled,
                actions.fields.onChange,
                imageLoader,
            )
        }
    val flow = rememberComposerSendFlow(state, actions.onSubmit)
    val fields = availableComposerFields(state.rules, newThread, LocalComposerPreferences.current.hiddenFields)
    var showFields by remember(state.draft.resetId) { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = LocalFocusManager.current
    val density = LocalDensity.current
    var panelHeight by remember { mutableStateOf(72.dp) }
    val send = {
        focus.clearFocus()
        keyboard?.hide()
        when (flow.open(state, SystemClock.elapsedRealtime())) {
            CaptchaSendAction.SEND -> if (flow.consume()) actions.onSubmit()
            CaptchaSendAction.RELOAD -> actions.captcha.onReloadCaptcha()
            else -> Unit
        }
    }
    CompositionLocalProvider(
        LocalBottomOverlayInset provides panelHeight,
        LocalComposerImporting provides state.draft.filesLoading,
    ) {
        Column(Modifier.fillMaxSize().statusBarsPadding().focusedWindowBottomPadding()) {
            ComposerScreenHeader(
                title,
                state,
                enabled,
                actions.onClose,
                actions.fields.onChange,
                fields =
                    fields.takeIf { it.isNotEmpty() }?.let {
                        ComposerFieldsAccess(filledComposerFieldCount(state.draft, it)) { showFields = true }
                    },
            )
            ComposerScreenBody(state, imageLoader, actions.fields, enabled, picker, Modifier.weight(1f))
            ComposerEditorDock(
                state.copy(status = state.status.copy(canSubmit = state.status.canRequestSubmit)),
                newThread,
                picker.onAttach,
                send,
                Modifier.onSizeChanged { panelHeight = with(density) { it.height.toDp() } },
            )
        }
    }
    if (showFields) ComposerFieldsDialog(state, fields, actions.fields.onChange) { showFields = false }
    if (flow.visible) ComposerCaptchaDialog(state, flow, imageLoader, actions)
}

@Composable
private fun ComposerScreenBody(
    state: ComposerScreenState,
    imageLoader: ImageLoader,
    actions: ComposerFieldActions,
    enabled: Boolean,
    picker: ComposerAttachmentPickerState,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState(), overscrollEffect = null)
                .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        key(state.draft.resetId) {
            ComposerTextEditor(state.draft, state.rules, enabled, actions, picker.onReceive)
            composerUploadBudget(state.draft.attachments, state.rules).status?.let {
                Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(Modifier.padding(vertical = 8.dp)) {
                ComposerMediaSection(state.draft, imageLoader, enabled, picker.error, actions.onChange)
            }
        }
    }
}
