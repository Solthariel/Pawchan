package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.feature.composer.R

@Composable
fun NewThreadScreen(
    board: BoardId,
    state: NewThreadUiState,
    imageLoader: ImageLoader,
    actions: NewThreadActions,
) {
    ComposerEditorScreen(
        title = stringResource(R.string.new_thread_title, board.value),
        newThread = true,
        state =
            ComposerScreenState(
                state.draft,
                state.rules,
                state.captchaState,
                ComposerScreenStatus(state.progress, state.failure, state.canSubmit),
            ),
        imageLoader = imageLoader,
        actions = ComposerScreenActions(actions.fields, actions.captcha, actions.onSubmit, actions.onBack),
    )
}

data class NewThreadActions(
    val onBack: () -> Unit,
    val fields: ComposerFieldActions,
    val captcha: NewThreadCaptchaActions,
    val onSubmit: () -> Unit,
) {
    val onEmojiSelected: (Int) -> Unit
        get() = captcha.onEmojiSelected
    val onReloadCaptcha: () -> Unit
        get() = captcha.onReloadCaptcha
}

data class NewThreadCaptchaActions(
    val onEmojiSelected: (Int) -> Unit,
    val onReloadCaptcha: () -> Unit,
)
