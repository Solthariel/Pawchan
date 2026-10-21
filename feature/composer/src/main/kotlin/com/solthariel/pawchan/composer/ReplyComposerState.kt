package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
import com.solthariel.pawchan.domain.posting.PostingFailure
import com.solthariel.pawchan.domain.posting.PostingOptions
import com.solthariel.pawchan.domain.posting.PostingRules

data class ReplyComposerState(
    val visible: Boolean = false,
    val draft: ComposerDraftState = ComposerDraftState(),
    val captchaState: ComposerCaptchaState = ComposerCaptchaState(),
    val progress: ComposerProgressState = ComposerProgressState(),
    val failure: ComposerFailureState = ComposerFailureState(),
    val rules: PostingRules? = null,
) {
    val comment: String
        get() = draft.comment
    val attachments: List<ComposerAttachment>
        get() = draft.attachments
    val captcha: EmojiCaptchaChallenge?
        get() = captchaState.challenge
    val selectedEmojiImages: List<ByteArray>
        get() = captchaState.selectedImages
    val emojiCaptchaToken: String?
        get() = captchaState.token
    val captchaSatisfied: Boolean
        get() = captchaState.satisfied
    val captchaLoading: Boolean
        get() = progress.captchaLoading
    val captchaSelecting: Boolean
        get() = progress.captchaSelecting
    val submitting: Boolean
        get() = progress.submitting
    val error: ReplyError?
        get() = failure.error
    val rejectionReason: String?
        get() = failure.reason
    val canSubmit: Boolean
        get() =
            (comment.isNotBlank() || attachments.isNotEmpty()) &&
                comment.length <= (rules?.maxCommentLength ?: MAX_COMMENT_LENGTH) &&
                rules?.acceptsSelection(attachments.map { it.source }) == true &&
                !draft.filesLoading &&
                attachments.all { it.validName } &&
                captchaSatisfied &&
                !captchaLoading &&
                !captchaSelecting &&
                !submitting &&
                !progress.sent &&
                error == null

    companion object {
        const val MAX_COMMENT_LENGTH: Int = 15_000
    }
}

data class ComposerDraftState(
    val subject: String = "",
    val comment: String = "",
    val email: String = "",
    val attachments: List<ComposerAttachment> = emptyList(),
    val filesLoading: Boolean = false,
    val resetId: Int = 0,
    val options: PostingOptions = PostingOptions(),
) {
    val tag: String get() = options.tag
}

data class ComposerCaptchaState(
    val challenge: EmojiCaptchaChallenge? = null,
    val selectedImages: List<ByteArray> = emptyList(),
    val token: String? = null,
    val satisfied: Boolean = false,
    val deadlineElapsedMillis: Long? = null,
    val keyboard: List<EmojiCaptchaOption?> = challenge?.options.orEmpty(),
)

internal fun PostingFailure.toReplyError(): Pair<ReplyError, String?> =
    when (this) {
        PostingFailure.NetworkUnavailable -> ReplyError.NETWORK to null
        PostingFailure.NetworkTimeout -> ReplyError.TIMEOUT to null
        PostingFailure.SecureConnectionFailed -> ReplyError.SECURE_CONNECTION to null
        PostingFailure.InvalidResponse -> ReplyError.INVALID_RESPONSE to null
        PostingFailure.CaptchaRejected -> ReplyError.CAPTCHA to null
        PostingFailure.ThreadClosed -> ReplyError.THREAD_CLOSED to null
        is PostingFailure.HttpStatus -> ReplyError.SERVER to null
        is PostingFailure.Rejected -> ReplyError.REJECTED to reason
    }
