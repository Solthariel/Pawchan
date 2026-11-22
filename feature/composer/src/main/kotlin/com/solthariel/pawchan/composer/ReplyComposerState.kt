package com.solthariel.pawchan.composer

import com.solthariel.pawchan.domain.posting.EmojiCaptchaChallenge
import com.solthariel.pawchan.domain.posting.EmojiCaptchaOption
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
    val canRequestSubmit: Boolean
        get() =
            rules != null && failure.error != ReplyError.THREAD_CLOSED &&
                copy(
                    captchaState = ComposerCaptchaState(satisfied = true),
                    progress = progress.copy(captchaLoading = false, captchaSelecting = false),
                    failure = ComposerFailureState(),
                ).canSubmit

    val canSubmit: Boolean
        get() =
            (comment.isNotBlank() || attachments.isNotEmpty()) &&
                comment.length <= (rules?.maxCommentLength ?: MAX_COMMENT_LENGTH) &&
                preparedSelectionAllowed(attachments, rules) &&
                !draft.inputBlocked &&
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

/** Reset generations and asynchronous import tokens are transient, never part of the stored payload. */
data class ComposerDraftRevision(
    val resetId: Int = 0,
    val importId: Long = 0,
)

data class ComposerDraftState(
    val subject: String = "",
    val comment: String = "",
    val email: String = "",
    val attachments: List<ComposerAttachment> = emptyList(),
    val activity: ComposerDraftActivity = ComposerDraftActivity.IDLE,
    val revision: ComposerDraftRevision = ComposerDraftRevision(),
    val options: PostingOptions = PostingOptions(),
) {
    constructor(resetId: Int, options: PostingOptions = PostingOptions()) :
        this(revision = ComposerDraftRevision(resetId = resetId), options = options)

    val resetId: Int get() = revision.resetId
    val importId: Long get() = revision.importId

    internal fun withResetId(resetId: Int): ComposerDraftState = copy(revision = revision.copy(resetId = resetId))

    internal fun withImportId(
        id: Long,
        activity: ComposerDraftActivity = this.activity,
    ): ComposerDraftState = copy(activity = activity, revision = revision.copy(importId = id))

    internal fun acceptsChange(next: ComposerDraftState): Boolean =
        resetId == next.resetId &&
            when {
                next.importId == 0L -> !filesLoading || importId == 0L
                next.filesLoading -> !inputBlocked || importId == next.importId
                else -> filesLoading && importId == next.importId
            }

    internal fun committedEdit(): ComposerDraftState =
        copy(
            attachments = attachments.toList(),
            revision = if (filesLoading || importId == 0L) revision else revision.copy(importId = 0L),
        )

    val tag: String get() = options.tag
    val filesLoading: Boolean get() = activity == ComposerDraftActivity.LOADING_FILES
    val closing: Boolean get() = activity == ComposerDraftActivity.CLOSING
    val inputBlocked: Boolean get() = activity != ComposerDraftActivity.IDLE
}

data class ComposerCaptchaState(
    val challenge: EmojiCaptchaChallenge? = null,
    val selectedImages: List<ByteArray> = emptyList(),
    val token: String? = null,
    val satisfied: Boolean = false,
    val deadlineElapsedMillis: Long? = null,
    val keyboard: List<EmojiCaptchaOption?> = challenge?.options.orEmpty(),
)

/** Transient form activity is not persisted with the draft. */
enum class ComposerDraftActivity { IDLE, RESTORING, LOADING_FILES, RESTORE_FAILED, CLOSING }
