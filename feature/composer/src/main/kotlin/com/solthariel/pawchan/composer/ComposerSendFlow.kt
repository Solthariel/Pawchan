package com.solthariel.pawchan.composer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal enum class CaptchaSendAction { SEND, SHOW, RELOAD, WAIT }

internal fun captchaSendAction(
    state: ComposerScreenState,
    now: Long,
): CaptchaSendAction =
    when {
        state.status.progress.captchaLoading -> CaptchaSendAction.WAIT
        state.status.failure.error == ReplyError.SUBMISSION_UNCONFIRMED -> CaptchaSendAction.SHOW
        state.status.failure.retryDeadlineElapsedMillis != null -> CaptchaSendAction.SHOW
        state.captcha.isExpired(now) -> CaptchaSendAction.RELOAD
        state.status.canSubmit && state.captcha.satisfied -> CaptchaSendAction.SEND
        state.status.failure.error != null -> CaptchaSendAction.SHOW
        state.captcha.challenge == null && !state.captcha.satisfied -> CaptchaSendAction.RELOAD
        else -> CaptchaSendAction.SHOW
    }

/** A one-shot intent, never persisted with a draft or restored across window recreation. */
internal class ComposerSendFlow {
    var visible by mutableStateOf(false)
        private set
    var pending by mutableStateOf(false)
        private set
    var suppressedCaptcha: ComposerCaptchaState? by mutableStateOf(null)
        private set

    fun open(
        state: ComposerScreenState,
        now: Long,
    ): CaptchaSendAction {
        val action = captchaSendAction(state, now)
        visible = action != CaptchaSendAction.SEND
        pending = state.status.failure.error != ReplyError.SUBMISSION_UNCONFIRMED
        suppressedCaptcha = state.captcha.takeIf { action == CaptchaSendAction.RELOAD }
        return action
    }

    fun arm() {
        if (visible) pending = true
    }

    fun disarm() {
        pending = false
    }

    fun consume(): Boolean {
        if (!pending) return false
        pending = false
        return true
    }

    fun dismiss() {
        pending = false
        visible = false
        suppressedCaptcha = null
    }
}

@Composable
internal fun rememberComposerSendFlow(
    state: ComposerScreenState,
    onSubmit: () -> Unit,
): ComposerSendFlow {
    val flow = remember(state.draft.resetId) { ComposerSendFlow() }
    LaunchedEffect(state.captcha, state.status, flow.pending) {
        if (state.status.progress.sent) flow.dismiss()
        if (flow.visible && flow.pending) {
            if (state.status.canSubmit && !state.captcha.isExpired(android.os.SystemClock.elapsedRealtime())) {
                if (flow.consume()) onSubmit()
            } else if (state.status.failure.error != null &&
                !state.status.progress.captchaLoading && !state.status.progress.captchaSelecting
            ) {
                flow.disarm()
            }
        }
    }
    return flow
}
