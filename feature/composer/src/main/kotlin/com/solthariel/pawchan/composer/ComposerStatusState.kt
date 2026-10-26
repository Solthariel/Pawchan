package com.solthariel.pawchan.composer

data class ComposerProgressState(
    val captchaLoading: Boolean = false,
    val captchaSelecting: Boolean = false,
    val submitting: Boolean = false,
    val sent: Boolean = false,
    val upload: com.solthariel.pawchan.domain.posting.PostingUploadProgress? = null,
)

data class ComposerFailureState(
    val error: ReplyError? = null,
    val reason: String? = null,
    val retryDeadlineElapsedMillis: Long? = null,
)

enum class ReplyError {
    NETWORK,
    TIMEOUT,
    SECURE_CONNECTION,
    INVALID_RESPONSE,
    SUBMISSION_UNCONFIRMED,
    CAPTCHA,
    THREAD_CLOSED,
    SERVER,
    REJECTED,
}
