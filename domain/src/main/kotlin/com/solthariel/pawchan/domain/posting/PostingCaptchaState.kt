package com.solthariel.pawchan.domain.posting

/** A skipped captcha may also carry server-verified passcode upload limits. */
data class PostingCaptchaState(
    val challenge: EmojiCaptchaChallenge?,
    val uploadLimits: AccountUploadLimits? = null,
)

data class AccountUploadLimits(
    val maxFiles: Int,
    val maxTotalFileBytes: Int,
) {
    init {
        require(maxFiles in 1..PostingRules.MAX_LOCAL_FILES)
        require(maxTotalFileBytes in 1..PostingRules.MAX_TOTAL_BYTES.toInt())
    }
}

fun PostingRules.withAccountLimits(limits: AccountUploadLimits?): PostingRules =
    if (limits == null || maxFiles == 0) {
        this
    } else {
        copy(
            maxFiles = limits.maxFiles,
            maxTotalFileBytes = limits.maxTotalFileBytes,
        )
    }
