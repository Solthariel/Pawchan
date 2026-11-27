package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import com.solthariel.pawchan.feature.composer.R

@Composable
fun ReplyComposer(
    state: ReplyComposerState,
    imageLoader: ImageLoader,
    actions: ReplyActions,
) {
    ComposerEditorScreen(
        title =
            stringResource(R.string.composer_reply_title),
        newThread = false,
        state =
            ComposerScreenState(
                state.draft,
                state.rules,
                state.captchaState,
                ComposerScreenStatus(
                    state.progress,
                    state.failure,
                    state.canSubmit,
                    canRequestSubmit = state.canRequestSubmit,
                ),
            ),
        imageLoader = imageLoader,
        actions =
            ComposerScreenActions(
                actions.fields,
                NewThreadCaptchaActions(
                    actions.onEmojiSelected,
                    actions.onReloadCaptcha,
                ),
                actions.onSubmit,
                actions.onClose,
            ),
    )
}

data class ReplyActions(
    val fields: ComposerFieldActions,
    val onEmojiSelected: (Int) -> Unit,
    val onReloadCaptcha: () -> Unit,
    val onSubmit: () -> Unit,
    val onClose: () -> Unit,
)
