package com.solthariel.pawchan.domain.posting

/** A skipped captcha may also carry server-verified passcode upload limits. */
data class PostingCaptchaState(
    val challenge: EmojiCaptchaChallenge?,
    val uploadLimits: AccountUploadLimits? = null,
)

data class AccountUploadLimits(
    val maxFiles: Int,
    val maxFileBytes: Int,
) {
    init {
        require(maxFiles in 1..PostingRules.MAX_LOCAL_FILES)
        require(maxFileBytes in 1..ReplyAttachment.MAX_ATTACHMENT_BYTES)
    }
}

fun PostingRules.withAccountLimits(limits: AccountUploadLimits?): PostingRules =
    if (limits == null || maxFiles == 0) {
        this
    } else {
        copy(
            maxFiles = limits.maxFiles,
            maxFileBytes = maxOf(maxFileBytes, limits.maxFileBytes),
        )
    }
