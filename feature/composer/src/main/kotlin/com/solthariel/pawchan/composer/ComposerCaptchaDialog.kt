package com.solthariel.pawchan.composer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import coil3.ImageLoader
import com.solthariel.pawchan.core.ui.PageLoadingIndicator
import com.solthariel.pawchan.feature.composer.R.string as L

@Composable
internal fun ComposerCaptchaDialog(
    state: ComposerScreenState,
    flow: ComposerSendFlow,
    imageLoader: ImageLoader,
    actions: ComposerScreenActions,
) {
    val res = LocalResources.current

    val busy = state.status.progress.submitting || state.status.progress.sent
    val dismiss = { if (!busy) flow.dismiss() }
    val captchaActions =
        NewThreadCaptchaActions(
            onEmojiSelected = {
                flow.arm()
                actions.captcha.onEmojiSelected(it)
            },
            onReloadCaptcha = {
                if (state.status.failure.error != ReplyError.SUBMISSION_UNCONFIRMED) flow.arm()
                actions.captcha.onReloadCaptcha()
            },
        )
    val suppressed = state.captcha === flow.suppressedCaptcha
    val shown = if (suppressed) ComposerCaptchaState() else state.captcha
    val status =
        if (suppressed) {
            state.status.copy(progress = ComposerProgressState(captchaLoading = true))
        } else {
            state.status
        }
    ComposerModal(dismiss) {
        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ComposerStatusLine(shown, status, captchaActions.onReloadCaptcha, imageLoader)
            if (status.progress.captchaLoading) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    PageLoadingIndicator(description = res.getString(L.ui_loading_captcha), size = 32.dp)
                }
            }
            if (!busy) ComposerCaptcha(shown, status.progress, imageLoader, captchaActions)
            if (!busy && !flow.pending && state.status.canSubmit) {
                TextButton(
                    onClick = { flow.arm() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(res.getString(L.ui_continue_sending)) }
            }
            TextButton(onClick = dismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(res.getString(L.ui_cancel))
            }
        }
    }
}
