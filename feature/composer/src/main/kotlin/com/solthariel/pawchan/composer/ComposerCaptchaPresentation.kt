package com.solthariel.pawchan.composer

internal class ComposerCaptchaPresentation(
    val data: ComposerCaptchaState,
    val progress: ComposerProgressState,
) {
    val captcha get() = data.challenge
    val selectedEmojiImages get() = data.selectedImages
    val captchaSelecting get() = progress.captchaSelecting
    val selectionEnabled get() = !progress.captchaSelecting && !progress.submitting && !progress.sent
}
