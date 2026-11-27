package com.solthariel.pawchan.composer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.solthariel.pawchan.core.ui.APP_TOOLBAR_BUTTON_SIZE
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.feature.composer.R
import com.solthariel.pawchan.feature.composer.R.string as L

/** Two independent actions inside one capsule; only send ripples across its full width. */
@Composable
internal fun ComposerActionPanel(
    state: ComposerScreenState,
    newThread: Boolean,
    onAttach: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sendInteraction = remember { MutableInteractionSource() }
    val progress = state.status.progress
    val colors = ButtonDefaults.buttonColors()
    val canSubmit = state.status.canSubmit && !state.draft.inputBlocked && !progress.submitting && !progress.sent
    val canAttach =
        !progress.submitting && !progress.sent && !state.draft.inputBlocked &&
            canAttachComposerMedia(state.draft, state.rules)
    var appearance by remember(state.draft.resetId) { mutableStateOf(canSubmit to canAttach) }
    if (!state.draft.filesLoading) SideEffect { appearance = canSubmit to canAttach }
    val paint = if (state.draft.filesLoading) appearance else canSubmit to canAttach
    val background = if (paint.first) MaterialTheme.colorScheme.surfaceContainerHigh else colors.disabledContainerColor
    val foreground = if (paint.first) MaterialTheme.colorScheme.onSurface else colors.disabledContentColor
    Box(modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .testTag("composer-action-capsule")
                .clip(CircleShape)
                .background(background)
                .indication(sendInteraction, ripple())
                .padding(4.dp)
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .testTag("composer-send-action")
                    .clickable(
                        interactionSource = sendInteraction,
                        indication = null,
                        enabled = canSubmit,
                        role = Role.Button,
                        onClick = onSubmit,
                    ).padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth().padding(start = APP_TOOLBAR_BUTTON_SIZE),
                    contentAlignment = Alignment.Center,
                ) {
                    ComposerSendLabel(progress.submitting, newThread, foreground)
                }
            }
            ComposerAttachButton(
                enabled = canAttach,
                onClick = onAttach,
                preserveAppearance = state.draft.filesLoading && paint.second,
            )
        }
    }
}

@Composable
private fun ComposerSendLabel(
    submitting: Boolean,
    newThread: Boolean,
    foreground: androidx.compose.ui.graphics.Color,
) {
    val res = LocalResources.current

    if (submitting) {
        PageLoadingIndicator(
            description = res.getString(L.ui_sending_message),
            size = 20.dp,
            backgroundColor = MaterialTheme.colorScheme.onSurface,
            foregroundColor = MaterialTheme.colorScheme.surface,
        )
    } else {
        Text(
            stringResource(if (newThread) R.string.new_thread_create else R.string.reply_send),
            color = foreground,
            style = MaterialTheme.typography.labelLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
