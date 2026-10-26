package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import coil3.ImageLoader
import com.solthariel.pawchan.domain.model.BoardId
import com.solthariel.pawchan.feature.composer.R

@Composable
fun ReplyComposer(
    board: BoardId?,
    state: ReplyComposerState,
    imageLoader: ImageLoader,
    actions: ReplyActions,
) {
    ComposerEditorScreen(
        title =
            board?.let { stringResource(R.string.new_message_title, it.value) }
                ?: stringResource(R.string.composer_reply_title),
        newThread = false,
        state =
            ComposerScreenState(
                state.draft,
                state.rules,
                state.captchaState,
                ComposerScreenStatus(state.progress, state.failure, state.canSubmit),
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

val ReplyError.messageResource: Int
    get() =
        when (this) {
            ReplyError.NETWORK -> R.string.error_network
            ReplyError.TIMEOUT -> R.string.error_timeout
            ReplyError.SECURE_CONNECTION -> R.string.error_secure_connection
            ReplyError.SUBMISSION_UNCONFIRMED -> R.string.reply_error_unconfirmed
            ReplyError.INVALID_RESPONSE -> R.string.error_invalid_response
            ReplyError.CAPTCHA -> R.string.reply_error_captcha
            ReplyError.THREAD_CLOSED -> R.string.thread_closed
            ReplyError.SERVER -> R.string.error_server
            ReplyError.REJECTED -> R.string.reply_error_rejected
        }
