package com.solthariel.pawchan.composer

data class ComposerProgressState(
    val captchaLoading: Boolean = false,
    val captchaSelecting: Boolean = false,
    val submitting: Boolean = false,
    val sent: Boolean = false,
)

data class ComposerFailureState(
    val error: ReplyError? = null,
    val reason: String? = null,
)

enum class ReplyError {
    NETWORK,
    TIMEOUT,
    SECURE_CONNECTION,
    INVALID_RESPONSE,
    CAPTCHA,
    THREAD_CLOSED,
    SERVER,
    REJECTED,
}
